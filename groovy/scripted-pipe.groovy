node('masterLin') {
    properties([
        buildDiscarder(logRotator(daysToKeepStr: '-1', numToKeepStr: '5')),
        parameters([
            [$class: 'ChoiceParameter',
                name: 'stend',
                randomName: 'choice-parameter-3151808045461422',
                description :'<BR><font size=3 color=red>Среда тестирования или контур стенда.</font> <BR>Соответствует файлу инвентаризации в каталоге /ansible/inventory.<BR>',
                choiceType: 'PT_SINGLE_SELECT',
                filterable: false,
                script: [
                    $class: 'GroovyScript',
                    script: [
                        classpath: [],
                        sandbox: true,
                        script: 
                        '''
                            // GitLab Settings
                            import groovy.json.*
                            import jenkins.model.*

                            try {
                                    def gitUrl = 'https://github.com/api/v4/projects/4197'
                                    def privateToken = 'h8jwxtGC-fRudcMNzs6b'

                                    // Reading inventory content from project
                                    def inventoryFilesUrl = new URL("${gitUrl}/repository/tree?path=ansible/inventory&private_token=${privateToken}")
                                    def stendList = new JsonSlurper().parse(inventoryFilesUrl).collect{it.name.split('\\\\.')[1]}

                                return stendList

                            } catch (Ex) {
                                def err = "Error: "+Ex.toString()
                                return [err]
                            }
                        '''
                    ],
                    fallbackScript: [
                        classpath: [],
                        sandbox: true,
                        script: 'return ["No testing environment found in repository"]'
                    ]
                ]
            ],
            [$class: 'CascadeChoiceParameter',
                name: 'hostGroups',
                randomName: 'choice-parameter-3158894612827986',
                description :'<BR><font size=3 color=red>Группы хостов в выбранном файле инвентаризации.</font> <BR>Определяет группу хостов, передаваемую в --limit="param1,param2 ..." при запуске Ansible. <BR><BR><strong>Важно:</strong> Если не установлена, то задание будет выполнено на всех хостах в файле инвентаризации.',
                choiceType: 'PT_CHECKBOX',
                referencedParameters: 'stend',
                filterable: false,
                script: [
                    $class: 'GroovyScript',
                    script: [
                        classpath: [],
                        sandbox: true,
                        script: 
                        '''
                            import groovy.json.* 
                            
                            try { // GitLab Settings 
                                def gitUrl = 'https://sbt-gitlab.ca.sbrf.ru/api/v4/projects/4197' 
                                def privateToken = 'h8jwxtGC-fRudcMNzs6b' 
                                
                                // Reading inventory file from project 
                                def inventoryFileUrl = new URL("${gitUrl}/repository/files/ansible%2Finventory%2Fhosts%2E$stend/blame?ref=master&private_token=${privateToken}") 
                                def hostGroups = new JsonSlurper().parse(inventoryFileUrl).lines.flatten().findAll{ it =~ /^\\[.*\\]$/}.collect{ it.replaceAll('(\\\\[|\\\\]|\\\\:children)','')} 
                                
                                return hostGroups 
                            
                            } catch (Ex) { 
                                def err = "Error: "+Ex.toString() 
                                return [err] 
                            }
                        '''
                    ],
                    fallbackScript: [
                        classpath: [],
                        sandbox: true,
                        script: 'return ["No host group found in inventory file."]'
                    ]
                ]
            ],
            [$class: 'StringParameterDefinition',
                name: 'ansible_limit',
                description: '<BR><font size=3 color=red>Список индивидуальных хостов.</font> <BR>Параметр передающийся в --limit="param1 param2 ..." при запуске Ansible <BR>При пустом значении параметр не передается. <BR><BR><strong>Важно:</strong>Если установлена, то отменяет значение параметра <b>hostGroups</b>.',
                defaultValue: ''
            ],
            [$class: 'BooleanParameterDefinition',
                name: 'stop_server',
                description: '<BR><font size=3 color=red>Остановить сервер приложений.</font> <BR>Останавливает сервер приложений Wildfly на выбранных хостах.',
                defaultValue: 'false'
            ],
            [$class: 'BooleanParameterDefinition',
                name: 'start_server_wo_app',
                description: '<BR><font size=3 color=red>Запустить сервер без приложений.</font> <BR>Запускает сервер приложений Wildfly на выбранных хостах без приложений.',
                defaultValue: 'false'
            ],
            [$class: 'BooleanParameterDefinition',
                name: 'start_server',
                description: '<BR><font size=3 color=red>Запустить сервер приложений.</font> <BR>Запускает сервер приложений Wildfly на выбранных хостах.',
                defaultValue: 'false'
            ],
            [$class: 'BooleanParameterDefinition',
                name: 'drop_logs',
                description: '<BR><font size=3 color=red>Удалить серверные и модульные логи.</font> <BR>Удаляет серверные и модульные логи.',
                defaultValue: 'false'
            ],
            [$class: 'BooleanParameterDefinition',
                name: 'heap_dump',
                description: '<BR><font size=3 color=red>Снять heap dump.</font> <BR>Снимает heap dump на выбранных хостах и создаёт архив этих файлов.',
                defaultValue: 'false'
            ],
            [$class: 'BooleanParameterDefinition',
                name: 'thread_dump',
                description: '<BR><font size=3 color=red>Снять thread dump.</font> <BR>Снимает thread dump на выбранных хостах и создаёт архив этих файлов.',
                defaultValue: 'false'
            ],
            [$class: 'StringParameterDefinition',
                name: 'dump_dir',
                description: '<BR><font size=3 color=red>Каталог для файлов thread dump и heap dump.</font> <BR>Определяет каталог для файлов thread dump и heap dump на выбранных хостах.<BR><BR><strong>Важно:</strong> Если не установлен, то файлы будут помещены в родительский каталог Wildfly.',
                defaultValue: '',
                trim: 'true'
            ]
        ])// end of params
    ])// end of properties
    ansiColor('xterm'){

        stage("GitSCM"){
            checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'gitlab_di', url: 'git@sbt-gitlab.delta.sbrf.ru:Vest-AV/support_vis.git']]])
        }
        dir("./ansible/") {

            limit = "${hostGroups}"
            if (ansible_limit != "") {
                limit = "${ansible_limit}"
            }


            timestamps {
                stage("Dump and Stack") {

                    def extraParams = ""
                    def skippedTags = []

                    if (heap_dump == "false") {
                        skippedTags.add("heapdump")
                    }

                    if (thread_dump == "false") {
                        skippedTags.add("jstack")
                    }

                    if (dump_dir != "") {
                       extraParams = "-e dump_dir=" + "${dump_dir}"
                    } 

                    if (heap_dump == "true" || thread_dump == "true" ) {
                        ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.$stend', limit: limit, playbook: 'heap-dump.yaml', vaultCredentialsId: 'vis_general_secret_text', extras: extraParams, skippedTags: skippedTags.join(",")
                    }
                }// end of stage
                stage("Stop servers") {
                    if (stop_server == "true") {
                        ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.$stend', limit: limit, playbook: 'stop_wf_service.yaml', vaultCredentialsId: 'vis_general_secret_text'
                    }
                }// end of stage
                stage("Drop logs") {
                    if (drop_logs == "true") {
                        ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.$stend', limit: limit, playbook: 'drop_logs.yaml', vaultCredentialsId: 'vis_general_secret_text'
                    }
                }// end of stage
                stage("Start servers without applications") {
                    if (start_server_wo_app == "true") {
                        ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.$stend', limit: limit, playbook: 'start_wf_core_service.yaml', vaultCredentialsId: 'vis_general_secret_text'
                    }
                }// end of stage
                stage("Start servers") {
                    if (start_server == "true") {
                        ansiblePlaybook installation: 'ansible26', disableHostKeyChecking: true, forks: 60, colorized: true, inventory: './inventory/hosts.$stend', limit: limit, playbook: 'start_wf_service.yaml', vaultCredentialsId: 'vis_general_secret_text'
                    }
                }// end of stage
            }// end of timestamps
        } // end of dir
    }// end of ansiColor
} // end of node
