<template id="user-overview">
  <app-layout>
    <div class="card bg-light mb-3">
      <div class="card-header">
        <div class="row">
          <div class="col-6">
            Users
          </div>
          <div class="col" align="right">
            <button rel="tooltip" title="Add"
                    class="btn btn-info btn-simple btn-link"
                    :disabled="!isAdmin"
                    @click="toggleForm">
              <i class="fa fa-plus" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="card-body">
        <div class="mb-2" v-if="!isAdmin">
          <small class="text-muted">
            Admin role required to add, edit, or delete users. Set your role on the home page.
          </small>
        </div>
        <div :class="{ 'd-none': hideForm || !isAdmin}">
          <form id="addUser">
          <div class="input-group mb-3">
            <div class="input-group-prepend">
              <span class="input-group-text" id="input-user-name">Name</span>
            </div>
            <input type="text" class="form-control" v-model="formData.name" name="name" placeholder="Name"/>
          </div>
          <div class="input-group mb-3">
            <div class="input-group-prepend">
              <span class="input-group-text" id="input-user-email">Email</span>
            </div>
            <input type="email" class="form-control" v-model="formData.email" name="email" placeholder="Email"/>
          </div>
          </form>
          <button rel="tooltip" title="Update" class="btn btn-info btn-simple btn-link" @click="addUser()">Add User</button>
        </div>
      </div>
    </div>
    <div class="list-group list-group-flush">
      <div class="list-group-item d-flex align-items-start"
           v-for="(user,index) in users" v-bind:key="index">
        <div class="mr-auto p-2">
          <span><a :href="`/users/${user.id}`"> {{ user.name }} ({{ user.email }})</a></span>
        </div>
        <div class="p2">
          <a :href="`/users/${user.id}`">
            <button rel="tooltip" title="Update" class="btn btn-info btn-simple btn-link"
                    :disabled="!isAdmin">
              <i class="fa fa-pencil" aria-hidden="true"></i>
            </button>
            <button rel="tooltip" title="Delete" class="btn btn-info btn-simple btn-link"
                    :disabled="!isAdmin"
                    @click="deleteUser(user, index)">
              <i class="fas fa-trash" aria-hidden="true"></i>
            </button>
          </a>
        </div>
      </div>
    </div>
  </app-layout>
</template>


<script>
app.component("user-overview", {
  template: "#user-overview",
  data: () => ({
    users: [],
    formData: {},
    hideForm: true,
    role: "user"
  }),
  created() {
    this.loadRole();
    this.fetchUsers();
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
    toggleForm() {
      if (!this.isAdmin) {
        alert("Admin role required to add users.");
        return;
      }
      this.hideForm = !this.hideForm;
    },
    fetchUsers: function () {
      axios.get("/api/users")
          .then(res => this.users = res.data)
          .catch(() => alert("Error while fetching users"));
    },
    deleteUser: function (user, index) {
      if (!this.isAdmin) {
        alert("Admin role required to delete users.");
        return;
      }
      if (confirm('Are you sure you want to delete this user? This action cannot be undone.', 'Warning')) {
        //user confirmed delete
        const userId = user.id;
        const url = `/api/users/${userId}`;
        const headers = { "X-User-Role": this.role };
        axios.delete(url, { headers })
            .then(() =>
                //delete from the local state so Vue will reload list automatically
                this.users.splice(index, 1))
            .catch(function (error) {
              console.log(error)
            });
      }
    },
    addUser: function (){
      if (!this.isAdmin) {
        alert("Admin role required to add users.");
        return;
      }
      const url = `/api/users`;
      const headers = { "X-User-Role": this.role };
      axios.post(url,
          {
            name: this.formData.name,
            email: this.formData.email
          },
          { headers })
          .then(response => {
            this.users.push(response.data)
            this.hideForm= true;
          })
          .catch(error => {
            console.log(error)
          })
    }

  }
});
</script>
