/*
  Декларативный пайплайн. В виду ограничений на объявление параметров (ex.:ActiveChoiceParameter) не используется.
  Оставлен как пример.
*/
pipeline {
    agent { label 'masterLin' }
    options {
        timeout(time: 30, unit: 'MINUTES')
        ansiColor('xterm')
        buildDiscarder(logRotator(daysToKeepStr: '-1', numToKeepStr: '5'))
    }
    parameters {
        choice(
            name: 'hostGroups',
            choices: ['IFT_RB_SOS-DEPOSIT_CA1','IFT_RB_SOS-DEPOSIT_CA2','IFT_RB_PAYROLL-MASTER','IFT_RB_PAYROLL-SLAVE','IFT_RB_DPF-ACCOUNT_CA1','IFT_RB_DPF-ACCOUNT_CA2','IFT_RB_SOS-DEPOSIT','IFT_RB_DPF-ACCOUNT','IFT_RB_PAYROLL','IFT_RB_DPF-EDITOR','IFT_RB'],
            description: '<BR><font size=3 color=red>Группы хостов в выбранном файле инвентаризации.</font><BR>Определяет группу хостов, передаваемую в --limit="param1,param2 ..." при запуске Ansible.<BR><BR><strong>Важно:</strong> Если не установлена, то задание будет выполнено на всех хостах в файле инвентаризации.'
        )
        string(
            name: 'ansible_limit',
            description: '<BR><font size=3 color=red>Список индивидуальных хостов.</font> <BR>Параметр передающийся в --limit="param1 param2 ..." при запуске Ansible <BR>При пустом значении параметр не передается. <BR><BR><strong>Важно:</strong>Если установлена, то отменяет значение параметра <b>hostGroups</b>.',
            defaultValue: ''
        )
        booleanParam(
            defaultValue: false, 
            description: '<BR><font size=3 color=red>Остановить сервер приложений.</font> <BR>Останавливает сервер приложений Wildfly на выбранных хостах. <BR><BR><strong>Важно:</strong>После остановки сервера дополнительно отключает все приложения.',
            name: 'stop_server'
        )
        booleanParam(
            defaultValue: false, 
            description: '<BR><font size=3 color=red>Запустить сервер приложений.</font> <BR>Запускает сервер приложений Wildfly на выбранных хостах. <BR><BR><strong>Важно:</strong>Сервер запускается без приложений.',
            name: 'start_server'
        )
        booleanParam(
            defaultValue: false, 
            description: '<BR><font size=3 color=red>Запустить приложения.</font> <BR>Запускает все приложения на серверах Wildfly на выбранных хостах.',
            name: 'start_apps'
        )
    }
    stages {
        stage('init') {
            steps {
                checkout([
                    $class: 'GitSCM', 
                    branches: [[name: '*/master']], 
                    doGenerateSubmoduleConfigurations: false, 
                    extensions: [], 
                    submoduleCfg: [], 
                    userRemoteConfigs: [[credentialsId: '1061b759-bbea-4fd3-a949-ed1b919e1380', 
                                         url: 'git@sbt-gitlab.delta.sbrf.ru:Vest-AV/support_vis.git']]
                ])
                script {
                    stend = 'IFT_RB'
                    limit = "${params.hostGroups}"
                    if (params.ansible_limit != "") {
                        limit = "${params.ansible_limit}"
                    }
                }// end of script
            }// end of steps
        }// end of stage
        stage("Stop servers") {
            steps {
                script {
                    if ("${params.stop_server}" == "true") {
                        dir("${env.WORKSPACE}/ansible/") {
                            ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.'+stend, limit: limit, playbook: 'wf_test.yaml', vaultCredentialsId: 'vis_general_secret_text'
                        }
                    }
                }
            }
        }// end of stage
        stage("Start servers") {
            steps {
                script {
                    if ("${params.start_server}" == "true") {
                        dir("${env.WORKSPACE}/ansible/") {
                            ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.'+stend, limit: limit, playbook: 'wf_test.yaml', vaultCredentialsId: 'vis_general_secret_text'
                        }
                    }
                }
            }
        }// end of stage
        stage("Start applications") {
            steps {
                script {
                    if ("${params.start_apps}" == "true") {
                        dir("${env.WORKSPACE}/ansible/") {
                            ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.'+stend, limit: limit, playbook: 'wf_test.yaml', vaultCredentialsId: 'vis_general_secret_text'
                        }
                    }
                }
            }
        }// end of stage
    }// end of stages
}// end of pipeline
