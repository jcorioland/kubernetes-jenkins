#!/usr/bin/groovy

// load pipeline functions
// Requires pipeline-github-lib plugin to load library from github
@Library('github.com/lachie83/jenkins-pipeline@master')
def pipeline = new io.estrado.Pipeline()

podTemplate(label: 'jenkins-pipeline', containers: [
    containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave:2.62', args: '${computer.jnlpmac} ${computer.name}', workingDir: '/home/jenkins', resourceRequestCpu: '500m', resourceLimitCpu: '500m', resourceRequestMemory: '1024Mi', resourceLimitMemory: '1024Mi'),
    containerTemplate(name: 'docker', image: 'docker:1.12.6', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'maven', image: 'maven:3.5.0-jdk-8', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:v2.6.1', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.3', command: 'cat', ttyEnabled: true)
],
volumes:[
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
]){

  node ('jenkins-pipeline') {

    def pwd = pwd()
    def chart_dir = "${pwd}/charts/hellojava"
    def tags = [:]
    tags << ['master': 'latest']
    def docker_registry_url = "jcorioland.azurecr.io"
    def docker_email = "jucoriol@microsoft.com"
    def docker_repo = "hellojava"
    def docker_acct = "kubernetes"
    def jenkins_registry_cred_id = "acr_creds"

    // checkout sources
    checkout scm

    // set additional git envvars for image tagging
    pipeline.gitEnvVars()

    // Execute Maven build and tests
    stage ('Maven Build & Tests') {

      container ('maven') {
        sh "mvn install"
      }

    }

    // Test Helm deployment (dry-run)
    stage ('Helm test deployment') {

      container('helm') {

        // run helm chart linter
        pipeline.helmLint(chart_dir)

        // run dry-run helm chart installation
        pipeline.helmDeploy(
          dry_run       : true,
          name          : "hello-java",
          namespace     : "hello-java",
          version_tag   : tags.get(0),
          chart_dir     : chart_dir,
          replicas      : 2,
          cpu           : "10m",
          memory        : "128Mi",
          hostname      : "hellojava.k8s-engine.jcorioland.io"
        )

      }
    }

    // Build and push the Docker image
    stage ('Build & Push Docker image') {

      container('docker') {
        println "build & push"

        // perform docker login
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: jenkins_registry_cred_id, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
          sh "docker login -e ${docker_email} -u ${env.USERNAME} -p ${env.PASSWORD} ${docker_registry_url}"
        }

        // build and publish container
        pipeline.containerBuildPub(
            dockerfile: "./",
            host      : docker_registry_url,
            acct      : docker_acct,
            repo      : docker_repo,
            tags      : tags,
            auth_id   : jenkins_registry_cred_id
        )
      }
    }
    
    // Deploy the new version to Kubernetes
    stage ('Deploy to Kubernetes') {
        container('helm') {

          // Deploy using Helm chart
           pipeline.helmDeploy(
            dry_run       : false,
            name          : "hello-java",
            namespace     : "hello-java",
            version_tag   : tags.get(0),
            chart_dir     : chart_dir,
            replicas      : 2,
            cpu           : "10m",
            memory        : "128Mi",
            hostname      : "hellojava.k8s-engine.jcorioland.io"
          )
        }
      }
  }
}