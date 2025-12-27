<!-- the "home-page" element is passed as a parameter to VueComponent in the JavalinConfig file -->
<template id="home-page">
  <app-layout>
    <div class="row mb-4">
      <div class="col-12">
        <div class="card mordor-hero">
          <div class="card-body">
            <h3 class="card-title mb-3">The Road to Mordor</h3>
            <p class="card-text mb-2">
              The fellowship has taken <strong>{{ fellowshipSteps }}</strong> steps towards Mount Doom.
            </p>
            <p class="mb-2" v-if="currentMilestone">
              The fellowship is passing <strong>{{ currentMilestone.name }}</strong>.
            </p>
            <div class="mordor-progress" v-if="fellowshipSteps > 0">
              <div class="progress">
                <div class="progress-bar" role="progressbar"
                     :style="{ width: fellowshipProgressPercent + '%' }"
                     :aria-valuenow="fellowshipProgressPercent" aria-valuemin="0" aria-valuemax="100">
                  {{ fellowshipProgressPercent }}%
                </div>
              </div>
              <small class="text-muted">Goal: 1,800,000 steps from the Shire to Mordor</small>
            </div>
            <p class="text-muted mb-0" v-else>
              No steps logged yet. Take your first steps from the Shire.
            </p>
            <div class="mt-3">
              <div class="input-group">
                <span class="input-group-text" id="input-role">Role</span>
                <select class="form-select" id="role-select" v-model="role" @change="persistRole">
                  <option value="user">User</option>
                  <option value="admin">Administrator</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row mb-4">
      <div class="col-12">
        <div class="card mordor-chart">
          <div class="card-header d-flex flex-column flex-md-row justify-content-between gap-2">
            <div>
              <div class="chart-title">Distance walked over time</div>
              <small class="text-muted">Cumulative distance from journeys and logged steps.</small>
            </div>
            <div class="chart-goal text-muted">
              Goal: {{ formatDistance(goalDistanceKm, 0) }} km
            </div>
          </div>
          <div class="card-body">
            <div v-if="distanceSeries.length === 0" class="text-muted">
              No distance logged yet. Add a journey to see progress.
            </div>
            <div v-else>
              <div class="chart-shell">
                <svg class="mordor-line-chart" viewBox="0 0 640 200" role="img" aria-label="Distance walked over time">
                  <defs>
                    <linearGradient id="mordorLineFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="rgba(243, 156, 18, 0.35)" />
                      <stop offset="100%" stop-color="rgba(243, 156, 18, 0)" />
                    </linearGradient>
                  </defs>
                  <g class="chart-grid">
                    <line v-for="(gridY, index) in chartGridLines" :key="`grid-${index}`"
                          x1="32" x2="608" :y1="gridY" :y2="gridY" />
                  </g>
                  <line v-if="goalLineY !== null"
                        class="chart-goal-line"
                        x1="32" x2="608" :y1="goalLineY" :y2="goalLineY" />
                  <path class="chart-area" :d="chartAreaPath"></path>
                  <path class="chart-line" :d="chartLinePath"></path>
                  <circle v-if="chartLastPoint" class="chart-point"
                          :cx="chartLastPoint.x" :cy="chartLastPoint.y" r="4" />
                </svg>
                <div class="chart-labels d-flex justify-content-between small text-muted">
                  <span>{{ chartStartLabel }}</span>
                  <span>{{ chartEndLabel }}</span>
                </div>
              </div>
              <div class="chart-stats mt-3">
                <div class="chart-stat">
                  <div class="stat-label">Total distance</div>
                  <div class="stat-value">{{ formatDistance(totalDistanceKm) }} km</div>
                </div>
                <div class="chart-stat">
                  <div class="stat-label">Average per day</div>
                  <div class="stat-value">{{ formatDistance(averageDailyDistanceKm) }} km</div>
                </div>
                <div class="chart-stat">
                  <div class="stat-label">Projected arrival</div>
                  <div class="stat-value">{{ projectedArrivalText }}</div>
                </div>
              </div>
              <small class="text-muted">
                Projection uses the average distance per day across all logged activity dates.
              </small>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row">
      <div class="col">
        <div class="card">
          <h5 class="card-header">Fellowship Companions</h5>
          <div class="card-body">
            <h5 class="card-title">{{users.length}} users</h5>
            <a href="/users" class="btn btn-primary">More Details...</a>
          </div>
        </div>
      </div>
      <div class="col">
        <div class="card">
          <h5 class="card-header">Journeys Logged</h5>
          <div class="card-body">
            <h5 class="card-title">{{activities.length}} journeys</h5>
            <a href="/activities" class="btn btn-primary">View Journeys</a>
          </div>
        </div>
      </div>
      <div class="col">
        <div class="card">
          <h5 class="card-header">Milestones to Mordor</h5>
          <div class="card-body">
            <h5 class="card-title">{{milestones.length}} milestones</h5>
            <a href="/milestones" class="btn btn-primary">View Milestones</a>
          </div>
        </div>
      </div>
    </div>
  </app-layout>
</template>

<script>
const STEPS_PER_KM = 1312;
const GOAL_STEPS = 1800000;
const CHART_WIDTH = 640;
const CHART_HEIGHT = 200;
const CHART_PADDING = 32;

app.component('home-page',
    {
      template: "#home-page",
      data: () => ({
        users: [],
        activities: [],
        milestones: [],
        role: "user"
      }),
      computed: {
        fellowshipSteps() {
          return this.activities.reduce((sum, a) => sum + (a.steps || 0), 0);
        },
        fellowshipProgressPercent() {
          const goal = 1800000;
          if (!goal) return 0;
          const pct = Math.round((this.fellowshipSteps / goal) * 100);
          return Math.min(100, pct);
        },
        currentMilestone() {
          if (!this.milestones || this.milestones.length === 0) return null;
          const steps = this.fellowshipSteps;
          const milestonesWithTargets = this.milestones
              .filter(m => m.targetSteps && m.targetSteps > 0)
              .sort((a, b) => a.targetSteps - b.targetSteps);

          if (milestonesWithTargets.length === 0) return null;

          let current = null;
          for (const m of milestonesWithTargets) {
            if (m.targetSteps <= steps) {
              current = m;
            } else {
              break;
            }
          }
          return current;
        },
        goalDistanceKm() {
          return GOAL_STEPS / STEPS_PER_KM;
        },
        distanceSeries() {
          const totals = new Map();
          for (const activity of this.activities) {
            const dateKey = this.activityDateKey(activity);
            if (!dateKey) continue;
            const distanceKm = this.activityDistanceKm(activity);
            if (!distanceKm) continue;
            totals.set(dateKey, (totals.get(dateKey) || 0) + distanceKm);
          }
          const keys = Array.from(totals.keys()).sort();
          let cumulative = 0;
          return keys.map(key => {
            const daily = totals.get(key) || 0;
            cumulative += daily;
            return {
              dateKey: key,
              dailyKm: daily,
              cumulativeKm: cumulative
            };
          });
        },
        totalDistanceKm() {
          if (this.distanceSeries.length === 0) return 0;
          return this.distanceSeries[this.distanceSeries.length - 1].cumulativeKm;
        },
        chartMaxDistanceKm() {
          const seriesMax = this.distanceSeries.reduce((max, item) => Math.max(max, item.cumulativeKm), 0);
          return Math.max(seriesMax, this.goalDistanceKm, 1);
        },
        chartPoints() {
          const series = this.distanceSeries;
          if (series.length === 0) return [];
          const innerWidth = CHART_WIDTH - CHART_PADDING * 2;
          const innerHeight = CHART_HEIGHT - CHART_PADDING * 2;
          const xStep = series.length > 1 ? innerWidth / (series.length - 1) : 0;
          return series.map((item, index) => {
            const x = CHART_PADDING + (xStep * index);
            const y = CHART_HEIGHT - CHART_PADDING - (item.cumulativeKm / this.chartMaxDistanceKm) * innerHeight;
            return { x, y, dateKey: item.dateKey };
          });
        },
        chartLinePath() {
          if (this.chartPoints.length === 0) return "";
          return this.chartPoints
              .map((point, index) => `${index === 0 ? "M" : "L"}${point.x} ${point.y}`)
              .join(" ");
        },
        chartAreaPath() {
          if (this.chartPoints.length === 0) return "";
          const first = this.chartPoints[0];
          const last = this.chartPoints[this.chartPoints.length - 1];
          const baseY = CHART_HEIGHT - CHART_PADDING;
          return `${this.chartLinePath} L ${last.x} ${baseY} L ${first.x} ${baseY} Z`;
        },
        chartLastPoint() {
          if (this.chartPoints.length === 0) return null;
          return this.chartPoints[this.chartPoints.length - 1];
        },
        chartGridLines() {
          const lines = 4;
          const innerHeight = CHART_HEIGHT - CHART_PADDING * 2;
          const step = innerHeight / lines;
          return Array.from({ length: lines + 1 }, (_, index) =>
              CHART_HEIGHT - CHART_PADDING - (step * index)
          );
        },
        goalLineY() {
          if (!this.chartMaxDistanceKm) return null;
          const innerHeight = CHART_HEIGHT - CHART_PADDING * 2;
          return CHART_HEIGHT - CHART_PADDING - (this.goalDistanceKm / this.chartMaxDistanceKm) * innerHeight;
        },
        chartStartLabel() {
          if (this.distanceSeries.length === 0) return "";
          return this.formatDateLabel(this.distanceSeries[0].dateKey);
        },
        chartEndLabel() {
          if (this.distanceSeries.length === 0) return "";
          return this.formatDateLabel(this.distanceSeries[this.distanceSeries.length - 1].dateKey);
        },
        averageDailyDistanceKm() {
          const days = this.activeDayCount;
          if (!days) return 0;
          return this.totalDistanceKm / days;
        },
        activeDayCount() {
          if (this.distanceSeries.length === 0) return 0;
          const first = new Date(this.distanceSeries[0].dateKey);
          const last = new Date(this.distanceSeries[this.distanceSeries.length - 1].dateKey);
          const diffMs = last.getTime() - first.getTime();
          const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
          return Math.max(1, diffDays + 1);
        },
        projectedArrivalText() {
          if (this.distanceSeries.length === 0) {
            return "Add journeys to estimate.";
          }
          if (this.totalDistanceKm >= this.goalDistanceKm) {
            return "Mordor reached!";
          }
          const avg = this.averageDailyDistanceKm;
          if (!avg) {
            return "Add more data to estimate.";
          }
          const remainingKm = this.goalDistanceKm - this.totalDistanceKm;
          const daysRemaining = Math.ceil(remainingKm / avg);
          const lastDate = new Date(this.distanceSeries[this.distanceSeries.length - 1].dateKey);
          const projected = new Date(lastDate.getTime() + daysRemaining * 24 * 60 * 60 * 1000);
          return `${this.formatDate(projected)} (~${daysRemaining} days)`;
        }
      },
      created() {
        this.loadRole();
        axios.get("/api/users")
            .then(res => this.users = res.data)
            .catch(() => alert("Error while fetching users"));
        axios.get("/api/activities")
            .then(res => this.activities = res.data)
            .catch(() => alert("Error while fetching activities"));
        axios.get("/api/milestones")
            .then(res => this.milestones = res.data)
            .catch(error => {
              if (error.response && error.response.status === 404) {
                // No milestones defined yet is not an error
                this.milestones = [];
              } else {
                console.log("Error while fetching milestones", error);
              }
            });
      },
      methods: {
        activityDateKey(activity) {
          const started = activity ? activity.started : null;
          if (!started) return null;
          if (typeof started === "string") {
            const parsed = new Date(started);
            if (!Number.isNaN(parsed.getTime())) {
              return parsed.toISOString().slice(0, 10);
            }
          }
          if (typeof started === "number") {
            const parsed = new Date(started);
            if (!Number.isNaN(parsed.getTime())) {
              return parsed.toISOString().slice(0, 10);
            }
          }
          if (typeof started === "object") {
            const millis = started.millis || started.iLocalMillis || started.iMillis;
            if (millis) {
              const parsed = new Date(millis);
              if (!Number.isNaN(parsed.getTime())) {
                return parsed.toISOString().slice(0, 10);
              }
            }
          }
          return null;
        },
        activityDistanceKm(activity) {
          const rawDistance = Number(activity?.distanceKm || 0);
          if (rawDistance > 0) return rawDistance;
          const steps = Number(activity?.steps || 0);
          if (steps > 0) return steps / STEPS_PER_KM;
          return 0;
        },
        formatDistance(distanceKm, digits = 2) {
          const safe = Number(distanceKm || 0);
          return safe.toFixed(digits);
        },
        formatDateLabel(dateKey) {
          if (!dateKey) return "";
          const parsed = new Date(dateKey);
          if (Number.isNaN(parsed.getTime())) return dateKey;
          return parsed.toLocaleDateString(undefined, { month: "short", day: "numeric" });
        },
        formatDate(date) {
          if (!date || Number.isNaN(date.getTime())) return "";
          return date.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
        },
        loadRole() {
          try {
            const storedRole = window.localStorage ? window.localStorage.getItem("mordorRole") : null;
            if (storedRole) {
              this.role = storedRole;
            }
          } catch (error) {
            console.log("Unable to load saved role", error);
          }
        },
        persistRole() {
          try {
            if (window.localStorage) {
              window.localStorage.setItem("mordorRole", this.role);
            }
          } catch (error) {
            console.log("Unable to persist role", error);
          }
        }
      }
    });
</script>

<style>
.mordor-hero {
  border: 1px solid rgba(255, 140, 0, 0.4);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4);
}

.mordor-chart .chart-title {
  font-weight: 600;
  letter-spacing: 0.03em;
}

.mordor-chart .chart-shell {
  background: radial-gradient(circle at top, rgba(255, 153, 0, 0.12), rgba(21, 21, 26, 0.1));
  border: 1px solid rgba(255, 140, 0, 0.2);
  border-radius: 12px;
  padding: 16px;
}

.mordor-line-chart {
  width: 100%;
  height: 220px;
}

.chart-grid line {
  stroke: rgba(255, 255, 255, 0.1);
  stroke-width: 1;
}

.chart-goal-line {
  stroke: rgba(231, 76, 60, 0.7);
  stroke-dasharray: 6 6;
  stroke-width: 2;
}

.chart-line {
  fill: none;
  stroke: #f39c12;
  stroke-width: 3;
}

.chart-area {
  fill: url(#mordorLineFill);
}

.chart-point {
  fill: #f39c12;
  stroke: #1c1c1f;
  stroke-width: 2;
}

.chart-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.chart-stat {
  background: rgba(30, 30, 36, 0.7);
  border: 1px solid rgba(255, 140, 0, 0.2);
  border-radius: 10px;
  padding: 12px 14px;
}

.chart-stat .stat-label {
  text-transform: uppercase;
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  color: rgba(248, 249, 250, 0.7);
}

.chart-stat .stat-value {
  font-weight: 600;
  font-size: 1.05rem;
}
</style>
