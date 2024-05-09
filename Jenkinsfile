pipeline {
  agent any
  stages {  
    stage('build'){
      steps{
        echo 'Building project...'
        sh "cat Jenkinsfile"
        sh "docker --version"
      }
    }
    stage('test Android'){
        steps{
          sh "Android test is running..."
        }
    }
    stage('test ServerGO'){
       steps{
          sh "ServerGO test is running..."
        }
    stage('test Backend'){
       steps{
          sh "Backend test is running..."
        }
    }
  }
}
