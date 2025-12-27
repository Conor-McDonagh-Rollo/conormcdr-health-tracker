<template id="milestone-overview">
  <app-layout>
    <div class="card bg-light mb-3">
      <div class="card-header">
        <div class="row">
          <div class="col-6">
            Milestones on the Road to Mordor
          </div>
          <div class="col" align="right">
            <button rel="tooltip" title="Add Milestone"
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
            Admin role required to add, edit, or delete milestones. Set your role on the home page.
          </small>
        </div>

        <div :class="{ 'd-none': hideForm || !isAdmin }" class="mb-3">
          <form id="addMilestone">
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-milestone-name">Name</span>
              </div>
              <input type="text" class="form-control" v-model="formData.name" name="name" placeholder="e.g. Rivendell"/>
            </div>
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-milestone-description">Description</span>
              </div>
              <input type="text" class="form-control" v-model="formData.description" name="description" placeholder="Resting place before the Misty Mountains"/>
            </div>
            <div class="input-group mb-2">
              <div class="input-group-prepend">
                <span class="input-group-text" id="input-milestone-steps">At steps</span>
              </div>
              <input type="number" min="0" class="form-control" v-model.number="formData.targetSteps" name="targetSteps" placeholder="e.g. 300000 (steps from the Shire)"/>
            </div>
          </form>
          <button rel="tooltip" :title="editingId ? 'Save Changes' : 'Add Milestone'"
                  class="btn btn-primary"
                  :disabled="!isAdmin"
                  @click="saveMilestone()">
            {{ editingId ? 'Save Changes' : 'Add Milestone' }}
          </button>
        </div>

        <p v-if="milestones.length === 0">
          No milestones have been defined yet. Add some key locations on the journey.
        </p>
        <ul v-else class="list-group list-group-flush">
          <li class="list-group-item d-flex justify-content-between align-items-start"
              v-for="(milestone, index) in milestones" :key="milestone.id">
            <div>
              <h5 class="mb-1">{{ milestone.name }}</h5>
              <p class="mb-1">{{ milestone.description }}</p>
              <small class="text-muted" v-if="milestone.targetSteps && milestone.targetSteps > 0">
                Reached after {{ milestone.targetSteps.toLocaleString() }} steps
              </small>
            </div>
            <div>
              <button rel="tooltip" title="Edit Milestone" class="btn btn-info btn-simple btn-link"
                      :disabled="!isAdmin"
                      @click="editMilestone(milestone, index)">
                <i class="fa fa-pencil" aria-hidden="true"></i>
              </button>
              <button rel="tooltip" title="Delete Milestone" class="btn btn-info btn-simple btn-link"
                      :disabled="!isAdmin"
                      @click="deleteMilestone(milestone, index)">
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
app.component("milestone-overview", {
  template: "#milestone-overview",
  data: () => ({
    milestones: [],
    formData: {},
    hideForm: true,
    editingId: null,
    editingIndex: null,
    role: "user"
  }),
  created() {
    this.loadRole();
    this.fetchMilestones();
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
    fetchMilestones() {
      axios.get("/api/milestones")
          .then(res => this.milestones = res.data)
          .catch(error => {
            if (error.response && error.response.status === 404) {
              this.milestones = [];
            } else {
              console.log("Error while fetching milestones", error);
              alert("Error while fetching milestones");
            }
          });
    },
    saveMilestone() {
      if (!this.isAdmin) {
        alert("Admin role required to modify milestones.");
        return;
      }
      const payload = {
        name: this.formData.name,
        description: this.formData.description,
        targetSteps: this.formData.targetSteps || 0
      };
      const headers = { "X-User-Role": this.role };

      if (this.editingId) {
        const url = `/api/milestones/${this.editingId}`;
        axios.patch(url, payload, { headers })
            .then(() => {
              this.editingId = null;
              this.editingIndex = null;
              this.formData = {};
              this.hideForm = true;
              this.fetchMilestones();
            })
            .catch(error => {
              console.log(error);
              alert("Error updating milestone");
            });
      } else {
        const url = `/api/milestones`;
        axios.post(url, payload, { headers })
            .then(response => {
              this.milestones.push(response.data);
              this.formData = {};
              this.hideForm = true;
            })
            .catch(error => {
              console.log(error);
              alert("Error adding milestone");
            });
      }
    },
    editMilestone(milestone, index) {
      if (!this.isAdmin) {
        return;
      }
      this.formData = {
        name: milestone.name,
        description: milestone.description,
        targetSteps: milestone.targetSteps
      };
      this.editingId = milestone.id;
      this.editingIndex = index;
      this.hideForm = false;
    },
    deleteMilestone(milestone, index) {
      if (!this.isAdmin) {
        alert("Admin role required to delete milestones.");
        return;
      }
      if (confirm('Are you sure you want to delete this milestone?', 'Warning')) {
        const id = milestone.id;
        const url = `/api/milestones/${id}`;
        const headers = { "X-User-Role": this.role };
        axios.delete(url, { headers })
            .then(() => this.milestones.splice(index, 1))
            .catch(error => {
              console.log(error);
              alert("Error deleting milestone");
            });
      }
    }
  }
});
</script>
