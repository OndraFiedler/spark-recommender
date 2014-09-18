package recommender

import cmd._
import inputdata.DataHolder

/**
 * Created by Ondra Fiedler on 23.7.14.
 */

/**
 * Holds used Recommender and DataHolder. Must be set up before using.
 */
object MainHolder extends Serializable {

  private var recommender: Option[Recommender] = None
  private var dataHolder: Option[DataHolder] = None

  /**
   * Sets up the recommender. This method has to be called before using of the Recommender.
   * @param conf Object with settings
   */
  def setUp(conf: Conf) {

    //Dataholder
    val dataHolderNameToFactoryMap = DataHolderFactoryFromConf.dataHolderFactories.map(holder => holder.getName -> holder).toMap
    val dataHolderStr: String = conf.data()
    dataHolder = Some(dataHolderNameToFactoryMap.get(dataHolderStr).get.getDataHolderInstance(conf))

    //Recommender
    val recommenderNameToFactoryMap = RecommenderFactoryFromConf.recommenderFactories.map(rec => rec.getName -> rec).toMap
    val recommenderStr: String = conf.method()
    recommender = Some(recommenderNameToFactoryMap.get(recommenderStr).get.getRecommender(conf))
  }

  def getRecommenderInstance(): Recommender = {
    recommender match {
      case Some(rec) => rec
      case None => throw new MainHolderNotInitializedException
    }
  }

  def getDataHolder(): DataHolder = {
    dataHolder match {
      case Some(holder) => holder
      case None => throw new MainHolderNotInitializedException
    }
  }

  class MainHolderNotInitializedException extends Exception

}