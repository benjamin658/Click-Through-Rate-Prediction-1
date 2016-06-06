package vote

import org.apache.spark.mllib.classification.{LogisticRegressionModel, SVMModel}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.rdd.RDD

/**
  * Created by benjamin658 on 2016/6/6.
  */
class ModelVote(lrModel: LogisticRegressionModel, svmModel: SVMModel, rdfModel: RandomForestModel) extends Serializable {
  def vote(dataSet: RDD[LabeledPoint]): RDD[(Double, Double)] = {
    val labelAndPreds = dataSet.map(point => {
      val lrPredict = lrModel.predict(point.features)
      val svmPredict = svmModel.predict(point.features)
      val rdfPredict = rdfModel.trees.map(tree => tree.predict(point.features)).filter(_ > 0).size.toDouble / rdfModel.numTrees
      val finalPredict = if (lrPredict + svmPredict + rdfPredict < 2) 0 else 1

      (finalPredict.toDouble, point.label)
    })

    labelAndPreds
  }

  def accurate(labelAndPreds: RDD[(Double, Double)]) = {
    val metrics = new BinaryClassificationMetrics(labelAndPreds)
    val auROC = metrics.areaUnderROC
    val auPRC = metrics.areaUnderPR
    val correctNum = labelAndPreds.filter(pair => pair._1 != pair._2).count()

    (auROC, auPRC, correctNum.toDouble)
  }
}
