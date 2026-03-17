pipeline {
    agent any

    stages {
        stage('Pull') {
            steps {
                git branch: 'develop-be',
                    credentialsId: 'gitlab-api-token',
                    url: 'https://lab.ssafy.com/s14-fintech-finance-sub1/S14P21A305.git'
            }
        }

        stage('Build') {
            steps {
                dir('be') {
                    sh 'docker build -t billage-be .'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    docker stop billage-be || true
                    docker rm billage-be || true
                    docker run -d \
                        --name billage-be \
                        -p 8080:8080 \
                        -v /etc/billage/application-prod.yml:/app/config/application-prod.yml \
                        billage-be
                '''
            }
        }
    }
}