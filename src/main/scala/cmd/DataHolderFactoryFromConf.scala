package cmd

import inputdata.{DataHolder, MovieLensDataHolder, NetflixInManyFilesDataHolder, NetflixInOneFileDataHolder}

/**
 * Created by Ondra Fiedler on 27.8.14.
 */


trait DataHolderFactoryFromConf extends NameAndDescription {
  /**
   * Returns an instance of DataHolder
   * @param conf Instance of Conf with parsed command line arguments
   * @return DataHolder instance
   */
  def getDataHolderInstance(conf: Conf): DataHolder
}

object DataHolderFactoryFromConf {
  /**
   * List of all objects that extend DataHolderFactoryFromConf. DataHolderFactoryFromConf object must be listed here in order to be used through command line interface.
   */
  val dataHolderFactories: List[DataHolderFactoryFromConf] = List(MovieLensDataHolderFactory, NetflixInOneFileDataHolderFactory, NetflixInManyFilesDataHolderFactory)
}

//Factories for datasets:

object MovieLensDataHolderFactory extends DataHolderFactoryFromConf {
  override def getName = "movieLens"

  override def getDescription = "MovieLens data - http://grouplens.org/datasets/movielens/\\" +
    "Ratings are stored in ratings.dat and movie titles in movies.dat"

  override def getDataHolderInstance(conf: Conf) = new MovieLensDataHolder(conf.dir())
}

object NetflixInOneFileDataHolderFactory extends DataHolderFactoryFromConf {
  override def getName = "netflix"

  override def getDescription = "Netflix data - http://www.netflixprize.com/\\" +
    "All ratings in file ratings.txt. Each line has format:\n<movieID>,<userID>,<rating>,<date>\\" +
    "Movie titles stored in movie_titles.txt"

  override def getDataHolderInstance(conf: Conf) = new NetflixInOneFileDataHolder(conf.dir())
}

object NetflixInManyFilesDataHolderFactory extends DataHolderFactoryFromConf {

  override def getName = "netflixInManyFiles"

  override def getDescription = "Netflix data - original data from Netflix Prize"

  override def getDataHolderInstance(conf: Conf) = new NetflixInManyFilesDataHolder(conf.dir())
}