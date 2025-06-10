package pcd.ass01;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class BoidsModel {

    private final List<Boid> boids = new ArrayList<>();
    private double separationWeight;
    private double alignmentWeight;
    private double cohesionWeight;
    private final double width;
    private final double height;
    private final double maxSpeed;
    private final double perceptionRadius;
    private final double avoidRadius;
    private final ReentrantLock separationLock, alignmentLock, cohesionLock;

    public BoidsModel(
            double initialSeparationWeight,
            double initialAlignmentWeight,
            double initialCohesionWeight,
            double width,
            double height,
            double maxSpeed,
            double perceptionRadius,
            double avoidRadius){
        separationWeight = initialSeparationWeight;
        alignmentWeight = initialAlignmentWeight;
        cohesionWeight = initialCohesionWeight;
        this.width = width;
        this.height = height;
        this.maxSpeed = maxSpeed;
        this.perceptionRadius = perceptionRadius;
        this.avoidRadius = avoidRadius;
        this.separationLock = new ReentrantLock();
        this.alignmentLock = new ReentrantLock();
        this.cohesionLock = new ReentrantLock();
    }

    public void setBoids(int nBoids) {
        boids.clear();
        for (int i = 0; i < nBoids; i++) {
            P2d pos = new P2d(-width/2 + Math.random() * width, -height/2 + Math.random() * height);
            V2d vel = new V2d(Math.random() * maxSpeed/2 - maxSpeed/4, Math.random() * maxSpeed/2 - maxSpeed/4);
            boids.add(new Boid(pos, vel));
        }
    }

    public List<Boid> getBoids(){
        return boids;
    }

    public double getMinX() {
        return -width/2;
    }

    public double getMaxX() {
        return width/2;
    }

    public double getMinY() {
        return -height/2;
    }

    public double getMaxY() {
        return height/2;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public void setSeparationWeight(double value) {
        try {
            separationLock.lock();
            this.separationWeight = value;
        }
        finally {
            separationLock.unlock();
        }
    }

    public void setAlignmentWeight(double value) {
        try {
            alignmentLock.lock();
            this.alignmentWeight = value;
        }
        finally {
            alignmentLock.unlock();
        }
    }

    public void setCohesionWeight(double value) {
        try {
            cohesionLock.lock();
            this.cohesionWeight = value;
        }
        finally {
            cohesionLock.unlock();
        }
    }

    public double getSeparationWeight() {
        try {
            separationLock.lock();
            return separationWeight;
        }
        finally {
            separationLock.unlock();
        }
    }

    public double getCohesionWeight() {
        try {
            cohesionLock.lock();
            return cohesionWeight;
        }
        finally {
            cohesionLock.unlock();
        }
    }

    public double getAlignmentWeight() {
        try {
            alignmentLock.lock();
            return alignmentWeight;
        }
        finally {
            alignmentLock.unlock();
        }
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getAvoidRadius() {
        return avoidRadius;
    }

    public double getPerceptionRadius() {
        return perceptionRadius;
    }
}