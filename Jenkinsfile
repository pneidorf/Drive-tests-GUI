pipeline {
  agent any
  stages {
    stage('clonning repo'){
        steps{
            cleanWs()
            git branch: 'main', url: 'https://github.com/pneidorf/Drive-tests-GUI'
            sh "git submodule init"
            sh "git submodule update"
        }
    }
    stage('build'){
      steps{
        sh "ls"
        echo 'Building project...'
        sh "docker --version"
        sh "docker build --no-cache -t build_go -f ./Docker_img/BUILD/Build_Go_Back ."
        sh "docker build --no-cache -t build_react -f ./Docker_img/BUILD/Build_front_react ."
        sh "docker build --no-cache -t build_python -f ./Docker_img/BUILD/Build_Python_Back ."
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
