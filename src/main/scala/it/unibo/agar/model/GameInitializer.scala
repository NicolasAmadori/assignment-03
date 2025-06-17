package it.unibo.agar.model

import scala.util.Random

object GameInitializer:

  def initialPlayer(name: String, width: Int, height: Int, initialMass: Double = 120.0): Player =
    Player(name, Random.nextInt(width), Random.nextInt(height), initialMass)
  
  def initialFoods(numFoods: Int, width: Int, height: Int, initialMass: Double = 100.0): Seq[Food] =
    (1 to numFoods).map[Food](i => Food(s"f$i", Random.nextInt(width), Random.nextInt(height), initialMass))
