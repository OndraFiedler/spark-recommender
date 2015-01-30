# Spark Recommender

Scalable recommendation system written in [Scala](http://www.scala-lang.org/) using the [Apache Spark framework](https://spark.apache.org/).  

Implemented algorithms include:

* k-nearest neighbors
* k-nearest neighbors with clustering
* k-nearest neighbors with a cluster tree
* Alternating Least Squares (ALS) from Spark's [MLlib](https://spark.apache.org/docs/latest/mllib-collaborative-filtering.html)

This first version was created during the [eClub Summer Camp 2014](http://www.eclub.cvutmedialab.cz/) at [Czech Technical University](http://www.cvut.cz/).  
See the results of a benchmark and documentation in *reportAndDocumentation.pdf*

## Build

Spark Recommender is built with [Simple Build Tool (SBT)](http://www.scala-sbt.org/). Run command:
 
    sbt assembly
    
It creates the jar file in directory target/scala-2.10/.

## Run
The application can be run using the *spark-submit* script.

    cd target/scala-2.10/

    ‘$SPARK_HOME‘/bin/spark-submit --master local --driver-memory 2G --executor-memory 6G SparkRecommender-assembly-0.1.jar --class Boot (+ parameters of the recommender)
    
See [documentation of Spark](https://spark.apache.org/docs/latest/submitting-applications.html) for information about parameters of *spark-submit*.

### Parameters of the recommender

* Setting up API
     * `--interface  <arg>` Interface for setting up API (default = localhost)
     * `--port  <arg>` Port of interface for setting up API (default = 8080)
* Setting the dataset
     * `--data  <arg>` Type of dataset
     * `--dir  <arg>` Directory containing files of dataset
     
     Supported datasets: movieLens, netflix, netflixInManyFiles

* Setting the algorithm
     * `--method  <arg>` Algorithm
     * `-pkey=value \[key=value\]...` Parameters for algorithm
     
     Provided algorithms: kNN, kMeansClusteredKnn, clusterTreeKnn, als

* Other
     * `--products  <arg>` Maximal number of recommended products (default = 10)
     * `--help` Shows help
     * `--version` Shows version
     
See the documentation for parameters of a particular algorithm.

### Example
    ‘$SPARK_HOME‘/bin/spark-submit --master local --driver-memory 2G \
    --executor-memory 6G SparkRecommender-assembly-0.1.jar --class Boot\
    --data movieLens --dir /mnt/share/movieLens/ \
    --method kNN -p numberOfNeighbors=5

For simplification there's `example-run` script which sets some defaults. When running with netflix
datasets it expects to have following files located in `--dir`:

  * `ratings.txt`
  * `movie_titles.txt`

```
./example-run --data netflix --dir /mnt/share/datasets/netflix \
 --method kNN -p numberOfNeighbors=5 --port 9090

```

## API

### Request
API supports two operations:

* Recommend from user ID

        host:port/recommend/fromuserid/?id=<userID, Int>
        
    Example:
  
        http://localhost:8080/recommend/fromuserid/?id=97
        
        
* Recommend from ratings

         host:port/recommend/fromratings/?rating=<productID, Int>,<rating, Double>
         
     Example:
      
         http://localhost:8080/recommend/fromratings/?rating=98,4&rating=176,5&rating=616,5
         
### Response
The API returns the recommended products in form of JSON objects.

The JSON object for one recommendation looks like this:
    
    {
        "product" : productID
        "rating" : Prediction of rating for this product
        "name" : "Name of product"
    }
    

Example recommendation of three products:

    {"recommendations":[
        {"product":312,"rating":5.0,"name":"High Fidelity (2000)"},
        {"product":494,"rating":5.0,"name":"Monty Python's The Meaning of Life: Special Edition (1983)"},
        {"product":516,"rating":4.0,"name":"Monsoon Wedding (2001)"}
    ]}
