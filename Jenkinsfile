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
        echo 'Android test is running...'
      }
    }
    stage('test ServerGO'){
      steps{
        echo 'ServerGO test is running...'
      }
    }
    stage('test Backend'){
      steps{
        echo 'Backend test is running...'
      }
    }
  }
}
