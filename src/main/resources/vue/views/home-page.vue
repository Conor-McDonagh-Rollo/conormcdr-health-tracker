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
