pipeline {
    agent any

    stages {
        stage('Pull') {
            steps {
                git branch: 'develop-be',
                    credentialsId: 'gitlab-git-credentials',
                    url: 'https://lab.ssafy.com/s14-fintech-finance-sub1/S14P21A305.git'

                script {
                    env.GIT_AUTHOR = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
                }
            }
        }

        stage('Build') {
            steps {
                withCredentials([file(credentialsId: 'firebase-service-account', variable: 'FIREBASE_KEY')]) {
                    sh 'cp $FIREBASE_KEY be/src/main/resources/billage-firebase-adminsdk.json'
                }

                dir('be') {
                    sh 'docker build -t billage-be .'
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'mysql-url', variable: 'MYSQL_URL'),
                    string(credentialsId: 'mysql-username', variable: 'MYSQL_USERNAME'),
                    string(credentialsId: 'mysql-password', variable: 'MYSQL_PASSWORD'),
                    string(credentialsId: 'mongodb-uri', variable: 'MONGODB_URI'),
                    string(credentialsId: 'redis-host', variable: 'REDIS_HOST'),
                    string(credentialsId: 'redis-port', variable: 'REDIS_PORT'),
                    string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
                    string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET'),
                    string(credentialsId: 'kakao-client-id', variable: 'KAKAO_CLIENT_ID'),
                    string(credentialsId: 'kakao-client-secret', variable: 'KAKAO_CLIENT_SECRET'),
                    string(credentialsId: 'jpa-ddl-auto', variable: 'JPA_DDL_AUTO')
                ]) {
                    sh '''
                        docker stop billage-be || true
                        docker rm billage-be || true
                        docker run -d \
                            --name billage-be \
                            --restart unless-stopped \
                            -p 8080:8080 \
                            -e SPRING_DATASOURCE_URL=$MYSQL_URL \
                            -e SPRING_DATASOURCE_USERNAME=$MYSQL_USERNAME \
                            -e SPRING_DATASOURCE_PASSWORD=$MYSQL_PASSWORD \
                            -e SPRING_DATA_MONGODB_URI=$MONGODB_URI \
                            -e SPRING_DATA_REDIS_HOST=$REDIS_HOST \
                            -e SPRING_DATA_REDIS_PORT=$REDIS_PORT \
                            -e SPRING_DATA_REDIS_PASSWORD=$REDIS_PASSWORD \
                            -e JWT_SECRET=$JWT_SECRET \
                            -e KAKAO_CLIENT_ID=$KAKAO_CLIENT_ID \
                            -e KAKAO_CLIENT_SECRET=$KAKAO_CLIENT_SECRET \
                            -e JPA_DDL_AUTO=$JPA_DDL_AUTO \
                            billage-be
                        docker image prune -f
                    '''
                }
            }
        }
    }

    post {
        success {
            mattermostSend color: 'good',
                message: "💰 BE 빌리지 금고에 ${env.GIT_AUTHOR}님이 새로운 기능을 입금하셨습니다! 💰\n(${env.JOB_NAME} #${env.BUILD_NUMBER})"
        }
        failure {
            mattermostSend color: 'danger',
                message: "🚨 BE 빌리지 금고에 문제가 생겼습니다! 범인은...${env.GIT_AUTHOR}! 🚨\n로그를 확인해 주세요.\n(Build: #${env.BUILD_NUMBER})\n🔗 http://3.35.206.231:9090/job/${env.JOB_NAME}/${env.BUILD_NUMBER}/console"
        }
    }
}