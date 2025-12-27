<template id="activity-overview">
  <app-layout>
    <div class="card bg-light mb-3">
      <div class="card-header">
        Journeys to Mordor
      </div>
      <div class="card-body">
        <p v-if="activities.length === 0">
          No journeys have been logged yet. Take your first steps towards Mordor!
        </p>
        <div v-else>
          <p>
            Total journeys: {{ activities.length }}
          </p>
          <table class="table table-striped">
            <thead>
            <tr>
              <th>#</th>
              <th>Companion</th>
              <th>Description</th>
              <th>Duration (mins)</th>
              <th>Calories</th>
              <th>Steps</th>
              <th>Distance (km)</th>
              <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="activity in activities" :key="activity.id">
              <td>{{ activity.id }}</td>
              <td>
                <a :href="`/users/${activity.userId}`">
                  User {{ activity.userId }}
                </a>
              </td>
              <td>{{ activity.description }}</td>
              <td>{{ activity.duration }}</td>
              <td>{{ activity.calories }}</td>
              <td>{{ activity.steps || 0 }}</td>
              <td>{{ formatDistance(activity.distanceKm) }}</td>
              <td>
                <button rel="tooltip" title="Edit Journey" class="btn btn-info btn-simple btn-link"
                        :disabled="!isAdmin"
                        @click="startEdit(activity)">
                  <i class="fa fa-pencil" aria-hidden="true"></i>
                </button>
                <button rel="tooltip" title="Delete Journey" class="btn btn-info btn-simple btn-link"
                        :disabled="!isAdmin"
                        @click="deleteActivity(activity)">
                  <i class="fas fa-trash" aria-hidden="true"></i>
                </button>
              </td>
            </tr>
            </tbody>
          </table>

          <div v-if="editingActivity" class="mt-3">
            <h5>Edit Journey #{{ editingActivity.id }}</h5>
            <form>
              <div class="input-group mb-2">
                <div class="input-group-prepend">
                  <span class="input-group-text">Description</span>
                </div>
                <input type="text" class="form-control" v-model="editForm.description" />
              </div>
              <div class="input-group mb-2">
                <div class="input-group-prepend">
                  <span class="input-group-text">Duration (mins)</span>
                </div>
                <input type="number" min="0" step="0.1" class="form-control" v-model.number="editForm.duration" />
              </div>
              <div class="input-group mb-2">
                <div class="input-group-prepend">
                  <span class="input-group-text">Calories</span>
                </div>
                <input type="number" min="0" class="form-control" v-model.number="editForm.calories" />
              </div>
              <div class="input-group mb-2">
                <div class="input-group-prepend">
                  <span class="input-group-text">Steps</span>
                </div>
                <input type="number" min="0" class="form-control" v-model.number="editForm.steps" />
              </div>
              <div class="input-group mb-2">
                <div class="input-group-prepend">
                  <span class="input-group-text">Distance (km)</span>
                </div>
                <input type="number" min="0" step="0.01" class="form-control" v-model.number="editForm.distanceKm" readonly />
              </div>
            </form>
            <button class="btn btn-primary me-2" @click="saveActivity()">Save</button>
            <button class="btn btn-secondary" @click="cancelEdit()">Cancel</button>
          </div>
        </div>
      </div>
    </div>
  </app-layout>
</template>

<script>
app.component("activity-overview", {
  template: "#activity-overview",
  data: () => ({
    activities: [],
    editingActivity: null,
    editForm: {
      description: "",
      duration: 0,
      calories: 0,
      steps: 0,
      distanceKm: 0
    },
    role: "user"
  }),
  created() {
    this.loadRole();
    this.fetchActivities();
  },
  computed: {
    isAdmin() {
      return (this.role || "").toLowerCase() === "admin";
    }
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
    fetchActivities() {
      axios.get("/api/activities")
          .then(res => this.activities = res.data)
          .catch(() => alert("Error while fetching journeys"));
    },
    startEdit(activity) {
      if (!this.isAdmin) {
        alert("Admin role required to edit journeys.");
        return;
      }
      this.editingActivity = activity;
      this.editForm = {
        description: activity.description,
        duration: activity.duration,
        calories: activity.calories,
        steps: activity.steps || 0,
        distanceKm: activity.distanceKm || 0
      };
    },
    cancelEdit() {
      this.editingActivity = null;
    },
    saveActivity() {
      if (!this.editingActivity) return;
      if (!this.isAdmin) {
        alert("Admin role required to edit journeys.");
        return;
      }
      const a = this.editingActivity;
      const url = `/api/activities/${a.id}`;
      const headers = { "X-User-Role": this.role };
      axios.patch(url, {
        description: this.editForm.description,
        duration: this.editForm.duration,
        calories: this.editForm.calories,
        started: a.started,
        userId: a.userId,
        steps: this.editForm.steps || 0,
        distanceKm: this.editForm.distanceKm || 0
      }, { headers })
          .then(() => {
            this.editingActivity = null;
            this.fetchActivities();
          })
          .catch(error => {
            console.log(error);
            alert("Error updating journey");
          });
    },
    deleteActivity(activity) {
      if (!this.isAdmin) {
        alert("Admin role required to delete journeys.");
        return;
      }
      if (!confirm('Are you sure you want to delete this journey?', 'Warning')) {
        return;
      }
      const url = `/api/activities/${activity.id}`;
      const headers = { "X-User-Role": this.role };
      axios.delete(url, { headers })
          .then(() => this.fetchActivities())
          .catch(error => {
            console.log(error);
            alert("Error deleting journey");
          });
    },
    formatDistance(distanceKm) {
      if (!distanceKm) {
        return "-";
      }
      return Number(distanceKm).toFixed(2);
    }
  }
});
</script>
