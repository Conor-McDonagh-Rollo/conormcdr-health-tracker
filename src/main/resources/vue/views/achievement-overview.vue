<template id="achievement-overview">
  <app-layout>
    <div class="card bg-light mb-3">
      <div class="card-header">
        <div class="row">
          <div class="col-6">
            Achievements
          </div>
          <div class="col" align="right">
            <button rel="tooltip" title="Add Achievement"
                    class="btn btn-info btn-simple btn-link"
                    :disabled="!isAdmin"
                    @click="hideForm = !hideForm">
              <i class="fa fa-plus" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="card-body">
        <div class="mb-3" v-if="!isAdmin">
          <small class="text-muted">
            Admin role required to add, edit, or delete achievements. Set your role on the home page.
          </small>
        </div>

        <div :class="{ 'd-none': hideForm || !isAdmin }" class="mb-3">
          <form id="addAchievement">
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-achievement-name">Name</span>
              </div>
              <input type="text" class="form-control" v-model="formData.name" name="name" placeholder="e.g. First Mile"/>
            </div>
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-achievement-description">Description</span>
              </div>
              <input type="text" class="form-control" v-model="formData.description" name="description"
                     placeholder="Walked a full mile"/>
            </div>
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-achievement-distance">Distance (km)</span>
              </div>
              <input type="number" min="0" step="0.01" class="form-control" v-model.number="formData.targetDistanceKm"
                     name="targetDistanceKm" placeholder="e.g. 1.6"/>
            </div>
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-achievement-badge">Badge</span>
              </div>
              <input type="file" class="form-control" accept="image/*" @change="handleBadgeUpload"/>
            </div>
          </form>
          <button rel="tooltip" :title="editingId ? 'Save Changes' : 'Add Achievement'"
                  class="btn btn-primary"
                  :disabled="!isAdmin"
                  @click="saveAchievement()">
            {{ editingId ? 'Save Changes' : 'Add Achievement' }}
          </button>
        </div>

        <p v-if="achievements.length === 0">
          No achievements have been defined yet.
        </p>
        <ul v-else class="list-group list-group-flush">
          <li class="list-group-item d-flex justify-content-between align-items-start"
              v-for="(achievement, index) in achievements" :key="achievement.id">
            <div class="d-flex align-items-center">
              <img v-if="achievement.badgePath" :src="achievement.badgePath" alt="Badge"
                   class="me-3 achievement-badge"/>
              <div>
                <h5 class="mb-1">{{ achievement.name }}</h5>
                <p class="mb-1">{{ achievement.description }}</p>
                <small class="text-muted">
                  Unlock at {{ formatDistance(achievement.targetDistanceKm) }} km
                </small>
              </div>
            </div>
            <div>
              <button rel="tooltip" title="Edit Achievement" class="btn btn-info btn-simple btn-link"
                      :disabled="!isAdmin"
                      @click="editAchievement(achievement, index)">
                <i class="fa fa-pencil" aria-hidden="true"></i>
              </button>
              <button rel="tooltip" title="Delete Achievement" class="btn btn-info btn-simple btn-link"
                      :disabled="!isAdmin"
                      @click="deleteAchievement(achievement, index)">
                <i class="fas fa-trash" aria-hidden="true"></i>
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </app-layout>
</template>

<script>
app.component("achievement-overview", {
  template: "#achievement-overview",
  data: () => ({
    achievements: [],
    formData: {},
    hideForm: true,
    editingId: null,
    editingIndex: null,
    badgeFile: null,
    role: "user"
  }),
  created() {
    this.loadRole();
    this.fetchAchievements();
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
    fetchAchievements() {
      axios.get("/api/achievements")
          .then(res => this.achievements = res.data)
          .catch(error => {
            if (error.response && error.response.status === 404) {
              this.achievements = [];
            } else {
              console.log("Error while fetching achievements", error);
              alert("Error while fetching achievements");
            }
          });
    },
    handleBadgeUpload(event) {
      const files = event.target.files || [];
      this.badgeFile = files.length > 0 ? files[0] : null;
    },
    buildPayload() {
      const payload = new FormData();
      payload.append("name", this.formData.name || "");
      payload.append("description", this.formData.description || "");
      payload.append("targetDistanceKm", this.formData.targetDistanceKm || 0);
      if (this.badgeFile) {
        payload.append("badge", this.badgeFile);
      }
      return payload;
    },
    saveAchievement() {
      if (!this.isAdmin) {
        alert("Admin role required to modify achievements.");
        return;
      }
      if (!this.editingId && !this.badgeFile) {
        alert("Please upload a badge icon.");
        return;
      }
      const headers = { "X-User-Role": this.role };
      const payload = this.buildPayload();

      if (this.editingId) {
        const url = `/api/achievements/${this.editingId}`;
        axios.patch(url, payload, { headers })
            .then(() => {
              this.editingId = null;
              this.editingIndex = null;
              this.formData = {};
              this.badgeFile = null;
              this.hideForm = true;
              this.fetchAchievements();
            })
            .catch(error => {
              console.log(error);
              alert("Error updating achievement");
            });
      } else {
        const url = `/api/achievements`;
        axios.post(url, payload, { headers })
            .then(response => {
              this.achievements.push(response.data);
              this.formData = {};
              this.badgeFile = null;
              this.hideForm = true;
            })
            .catch(error => {
              console.log(error);
              alert("Error adding achievement");
            });
      }
    },
    editAchievement(achievement, index) {
      if (!this.isAdmin) {
        return;
      }
      this.formData = {
        name: achievement.name,
        description: achievement.description,
        targetDistanceKm: achievement.targetDistanceKm
      };
      this.badgeFile = null;
      this.editingId = achievement.id;
      this.editingIndex = index;
      this.hideForm = false;
    },
    deleteAchievement(achievement, index) {
      if (!this.isAdmin) {
        alert("Admin role required to delete achievements.");
        return;
      }
      if (confirm('Are you sure you want to delete this achievement?', 'Warning')) {
        const url = `/api/achievements/${achievement.id}`;
        const headers = { "X-User-Role": this.role };
        axios.delete(url, { headers })
            .then(() => this.achievements.splice(index, 1))
            .catch(error => {
              console.log(error);
              alert("Error deleting achievement");
            });
      }
    },
    formatDistance(distanceKm) {
      if (!distanceKm) {
        return "0.00";
      }
      return Number(distanceKm).toFixed(2);
    }
  }
});
</script>

<style>
.achievement-badge {
  width: 48px;
  height: 48px;
  object-fit: contain;
}
</style>
