workflow "Build and publish" {
  on = "push"
  resolves = ["Upload artifact"]
}

action "Assemble" {
  uses = "MrRamych/gradle-actions@master"
  args = "assemble"
}

action "Upload artifact" {
  uses = "actions/upload-artifact@9da9a3d797c6670a4e5207e787dcd288abfa46f9"
  needs = ["Assemble"]
  secrets = ["GITHUB_TOKEN"]
}
