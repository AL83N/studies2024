package com.team254.lib.trajectory.timing;

import com.team254.lib.geometry.Pose2dWithCurvature;

public interface TimingConstraint {
    double getMaxVelocity(Pose2dWithCurvature state);

    MinMaxAcceleration getMinMaxAcceleration(Pose2dWithCurvature state, double velocity);

    class MinMaxAcceleration {
        protected final double min_acceleration_;
        protected final double max_acceleration_;

        public static MinMaxAcceleration kNoLimits = new MinMaxAcceleration();

        public MinMaxAcceleration() {
            // No limits.
            min_acceleration_ = Double.NEGATIVE_INFINITY;
            max_acceleration_ = Double.POSITIVE_INFINITY;
        }

        public MinMaxAcceleration(double min_acceleration, double max_acceleration) {
            min_acceleration_ = min_acceleration;
            max_acceleration_ = max_acceleration;
        }

        public double min_acceleration() {
            return min_acceleration_;
        }

        public double max_acceleration() {
            return max_acceleration_;
        }

        public boolean valid() {
            return min_acceleration() <= max_acceleration();
        }
    }
}
