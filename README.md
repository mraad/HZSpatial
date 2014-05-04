HZSpatial
=========

Truly Spatially Enabling Very Large In-Memory Databases - Apache Hazelcast

## Description
This project consists of a set of modules and patches to spatially enable [Apache Halzecast](http://www.hazelcast.org/). In addition, the advent of the [MapReduce](http://research.google.com/archive/mapreduce-osdi04.pdf) API, enables us push logic to the data nodes to operate on "local" data.

Like all other key-value stores, Hazelcast relies on key indexing to quickly retrieve elements from its in-memory store. Again, like other stores with non-native spatial capabilities, they rely on the native lexicographical order of the index with [geohash](http://en.wikipedia.org/wiki/Geohash) encoding to store and search spatial elements. Though this encoding is very smart to rely on the native index capabilities, range searches have to take into account [z-order](http://en.wikipedia.org/wiki/Z-order_(curve)) boundary conditions, and storing non-point features can significantly increase the storage indexing.

The solution is to introduce a **native** non-lexicographical index order, in such a way that spatial indexing can be used when storing and querying spatial elements. One such indexing is the [QuadTree](http://en.wikipedia.org/wiki/Quadtree) and an implementation can be found in the [Esri Geometry API](https://github.com/Esri/geometry-api-java).

As of this writing, Hazelcast has a hardcoded definition of an index on a collection. Luckily, the ```Index``` is a public interface and just the definition is set to a concrete implementation ```IndexImpl```. This [patch]() minimally modifies the ```IndexService``` class, **without** breaking any of the existing unit tests. It relies on *convention vs. configuration* to define a custom index on a ```<map/>``` field declaration.

Here is an example in an XML configuration:

```
<map name="geomap">
  ...       
  <indexes>
    <index ordered="true">point@com.esri.SpatialIndex[-180,-90,180,90,16]</index>
  </indexes>
</map>
```

Note the ```@``` sign after the map field name index declaration, followed by a fully qualified class name, then with a set values as arguments in between braces ```[]```.

In the above example, the ```geomap``` map elements have a field named ```point```, that is indexed with an instance of ```com.esri.SpatialIndex``` that is constructed with the string ```-180,-90,180,90,16```.  The first 4 elements in the string are quad tree full extent, and the last element is the depth of the tree.

## Implementation and Usage

The ```SpatialIndex``` has a ```QuadTree``` instance to store and search spatial values by their ```Envelope2D``` references.  However, spatial operations (i.e. contains, intersects, crosses, etc..) rely the true ```Geometry``` reference. So an interface is defined be implemented by map elements, in such that they can be identified when added to an ```IMap```.


```
public interface SpatialValue
{
    public Envelope2D getEnvelope2D();
    public Geometry getGeometry();
}
```

The Hazelcast API can be used can be used as normal:

```
Config config = new Config();
HazelcastInstance h = Hazelcast.newHazelcastInstance(config);
IMap<Integer,SpatialValue> map = h.getMap("geomap");
map.put(0, new SpatialValueImpl(new Point(lon,lat)));
```

To find and filter elements from a map, the ```find``` method is invoked with an instance of ```Predicate```. Hazelcast comes with a set of built-in in lexicographical predicates, to take advantage of the new spatial index, a set of spatial predicates have to be used.

A ```SpatialPredicate``` interface is introduced:

```
public interface SpatialPredicate extends Comparable<Geometry>
{
    public Envelope2D getEnvelope2D();
    public boolean predicates(final Geometry geometry);
}
```

The envelope of the predicate is used by the quad tree to home onto the area of interest, and the predicate function is used to filter the spatial values based on their true geometries.


## Patch and Build Hazelcast

```
git clone https://github.com/hazelcast/hazelcast.git
cd hazelcast
patch < create_user_defined_index_on_a_map1.patch
patch < added_hazelcast_instance_aware_to_mapper.patch
mvn -DskipTests install
```

## Build and Install

After clone the project:

```
mvn install
```

This will create a library that is linked into two submodules ```HZServer``` and ```HZClient```.

### Start the server

```
cd HZServer
./start-all.sh
```

### Stop the server

```
cd HZServer
./stop-all.sh
```

### Loading and finding data

Download the [Armed Conflict Location and Event Data](http://www.acleddata.com/) for Africa [shapefile](http://www.acleddata.com/wp-content/uploads/2014/large-docs/ACLED_All_Africa_1997-2013.zip). Unzip the file into a folder and define an environment variable ```ACLED_HOME``` that points to that folder. There are about 86,000 features in the shapefile with time, latitude, longitude and other attributes.

```
cd HZClient
./mvn-points.sh
```

Once the data is loaded, you can search for all the conflicts in an extent:

```
./mvn-search.sh
```

If you look at the content of the script, it searches an area around Algeria and will print the date of the conflict, and the content of the ```ACTOR1``` and ```NOTES``` fields.

## Bring Program To Data

The latest version of Hazelcast introduces a MapReduce API. A user implements the ```Mapper``` and ```Reduce``` interfaces and submits them as part of ```Job``` to be executed on each "datanode".

As an example implementation, we will execute a density analysis based on set of polygons in a [honeycomb lattice](http://en.wikipedia.org/wiki/Honeycomb_lattice) format.  The density of each cell is the number of conflicts it covers geographically.

In a MapReduce paradigm, the mapper iterates over each conflicts and find spatially the cell id that it covered by and emits that cell id.  The reducer operates on each cell id and sums the associated emitted values.  The result is a list of cell id and count by cell.

### Load the cell data

```
cd HZClient
./mvn-polygons
```

This loads the cell polygons in shape file format in the data folder into a spatially enabled Hazelcast map.


### Executing MapReduce

```
mvn exec:java -Dexec.mainClass=com.esri.Main -Dexec.args="mr density.csv"
```

I had to patch Hazelcast to introspect ```Mapper``` instances for the ```HazelcastInstanceAware``` interface,  in such that it can reference other maps for example.

_Hazelcast Folks: If you are reading this, please tell me if there is a way to do this without the patch - thanks in advance._

In the above example, the ```density.csv``` file will have the following format:

|CELLID|POPULATION|
|------|----------|
|   123|        14|
|   ...|       ...|


The data can be imported into ArcMap for visualization:

![ACLED](https://dl.dropboxusercontent.com/u/2193160/ACLED.png)
