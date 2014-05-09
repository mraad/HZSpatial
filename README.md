## Abstract
This project consists of a set of modules and patches to index spatially typed fields such as points,lines and polygons in [Halzecast](http://www.hazelcast.org/) maps.
In addition, the advent of the [MapReduce](http://research.google.com/archive/mapreduce-osdi04.pdf) API **in** Hazelcast (GREAT Job guys) enables applications to push logic onto the edge nodes to operate on the "local" data. An example will be shown later on how the spatial index is used in a MapReduce application for hotspot visualization.

## Description
Like all other stores, Hazelcast relies on field indexing to quickly retrieve elements from its in-memory nodes. Due to the native lexicographical order of the index, applications encode in [geohash](http://en.wikipedia.org/wiki/Geohash) spatial elements for storage and searching. Though this encoding is very smart _and_ relies on the native index capabilities, range searches have to take into account <a href="http://en.wikipedia.org/wiki/Z-order_(curve)">Z-order</a> boundary conditions, and saving non-point features such as large polygons can significantly increase the storage indexing.

The solution is to introduce a **native** non-lexicographical index order, in such a way that a true spatial indexing can be used when storing and querying spatial elements. One such indexing is the [QuadTree](http://en.wikipedia.org/wiki/Quadtree) and an implementation can be found in the [Esri Geometry API](https://github.com/Esri/geometry-api-java).

As of this writing, Hazelcast has a hardcoded definition of an index on a collection. Luckily, the ```Index``` is a public interface and just the definition is set to a concrete implementation ```IndexImpl```. This [patch](https://github.com/mraad/HZSpatial/blob/master/create_user_defined_index_on_a_map1.patch) minimally modifies the ```IndexService``` class.
It relies on *convention vs. configuration* to define a custom index on a ```IMap``` field index declaration.

Here is an example in an XML configuration:

```
<map name="geomap">
  ...       
  <indexes>
    <index ordered="true">point@com.esri.SpatialIndex[-180,-90,180,90,16]</index>
  </indexes>
</map>
```

Note the ```@``` sign after the map field name index declaration, followed by a fully qualified class name, then with a set values as arguments in between the braces ```[]```.

In the above example, the ```geomap``` map elements have a field named ```point```, that is indexed with an instance of ```com.esri.SpatialIndex``` who is constructed with the string ```-180,-90,180,90,16```.
Inside ```SpatialIndex```, the string is tokenized based on the comma character, in such that the first 4 tokens are the QuadTree full extent, and the last token is the tree depth.

## Implementation and Usage

**NOTE: This code is NOT production ready yet, the spatial index implementation is a ReadOnly implementation**

The ```SpatialIndex``` has a ```QuadTree``` instance to store and search spatial values by their ```Envelope2D``` reference.
However, spatial operations (i.e. contains, intersects, crosses, etc..) rely on the true ```Geometry``` reference.
So an interface is defined to be implemented by map elements, in such that they can be identified when added to an ```IMap```.

**Note to the Hazelcast folks:** This is done **outside** of Hazelcast :-)

```
public interface SpatialValue
{
    public Envelope2D getEnvelope2D();
    public Geometry getGeometry();
}
```

The Hazelcast API can _still_ be used normally:

```
Config config = new Config();
HazelcastInstance h = Hazelcast.newHazelcastInstance(config);
IMap<Integer,SpatialValueImpl> map = h.getMap("geomap");
map.put(0, new SpatialValueImpl(new Point(lon,lat)));
```

To search and filter elements from a map, the ```find``` method is invoked with an instance of ```Predicate```.
Hazelcast comes with a set of built-in lexicographical predicates, to take advantage of the new spatial index, a set of spatial predicates have to be used.

A ```SpatialPredicate``` interface is introduced:

```
public interface SpatialPredicate extends Comparable<Geometry>
{
    public Envelope2D getEnvelope2D();
    public boolean predicates(final Geometry geometry);
}
```

The envelope of the predicate is used by the quad tree to home onto an area of interest, and the ```predicates``` function is used to filter the spatial values based on their true geometries.


## Patch and Build Hazelcast

The following patches introduces dynamic indexing to ```IndexService.java```, and lets ```MapCombineTask.java``` set a Hazelcast instance on a ```Mapper``` implementation.

```
git clone https://github.com/hazelcast/hazelcast.git
cd hazelcast
patch < create_user_defined_index_on_a_map1.patch
patch < added_hazelcast_instance_aware_to_mapper.patch
mvn -DskipTests install
```

## Build and Install

This project depends on the [Esri Geometry API](https://github.com/Esri/geometry-api-java) and my [Shapefile](https://github.com/mraad/Shapefile) library.
Make sure to clone and mvn install them in that order before proceeding.

```
git clone https://github.com/mraad/HZSpatial.git
cd HZSpatial
mvn install
```

This will create a library ```HZLib.jar``` that is linked into the two submodules ```HZServer``` and ```HZClient```.

### Start the hazelcast server

```
cd HZServer
./start-all.sh
```

### Stop the hazelcast server

```
cd HZServer
./stop-all.sh
```

### Loading and finding data

Download the [Armed Conflict Location and Event Data](http://www.acleddata.com/) for Africa [shapefile](http://www.acleddata.com/wp-content/uploads/2014/large-docs/ACLED_All_Africa_1997-2013.zip).
Unzip the file into a folder and define an environment variable named ```ACLED_HOME``` that points to that folder.
This is a tiny set with about 86,000 features in the shapefile with time, latitude, longitude and other attributes.

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
As an example implementation, we will execute a density analysis based on set of polygons in a [honeycomb lattice](http://en.wikipedia.org/wiki/Honeycomb_lattice) format.
The density of each cell is the number of conflicts it covers geographically.
In a MapReduce paradigm, the mapper iterates over each conflict and finds spatially the cell id that it is covered by and emits that cell id.
The reducer operates on each cell id and sums the associated emitted values.
The result is a list of cell id and count by cell.
This is the ubiquitous spatial Hello World in MapReduce :-)

### Loading the cell data

The ```data``` folder contains a [Shapefile](http://en.wikipedia.org/wiki/Shapefile) with polygon features in honeycomb format, where each honeycomb cell width is about one geographical degree.

```
cd HZClient
./mvn-polygons
```

This loads the cell polygons into a spatially enabled Hazelcast map.

### Executing MapReduce

```
mvn exec:java -Dexec.mainClass=com.esri.Main -Dexec.args="mr density.csv"
```

As mentioned previously, we had to patch Hazelcast to introspect ```Mapper``` instances for the ```HazelcastInstanceAware``` interface,  in such that it can reference other maps for example.

**Note to Hazelcast Folks:** Please tell me if there is a way to do this _without_ the patch - thanks in advance.

In the above example, the ```density.csv``` file will have the following format:

|CELLID|POPULATION|
|------|----------|
|   123|        14|
|   ...|       ...|


The CSV file can be imported into ArcMap, where it is joined with the imported cell polygon shapefile and symbolized for visualization:

![ACLED](https://dl.dropboxusercontent.com/u/2193160/ACLED.png)
