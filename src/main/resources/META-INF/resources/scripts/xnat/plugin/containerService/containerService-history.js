/*
 * web: containerServices-siteAdmin.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * History Table Generator for Container Services
 */

console.log('containerService-history.js');

var XNAT = getObject(XNAT || {});
XNAT.plugin = getObject(XNAT.plugin || {});
XNAT.plugin.containerService = getObject(XNAT.plugin.containerService || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function() {

    /* ================ *
     * GLOBAL FUNCTIONS *
     * ================ */

    var undefined,
        rootUrl = XNAT.url.rootUrl,
        restUrl = XNAT.url.restUrl,
        csrfUrl = XNAT.url.csrfUrl;

    function spacer(width) {
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function errorHandler(e, title, closeAll) {
        console.log(e);
        title = (title) ? 'Error Found: ' + title : 'Error';
        closeAll = (closeAll === undefined) ? true : closeAll;
        var errormsg = (e.statusText) ? '<p><strong>Error ' + e.status + ': ' + e.statusText + '</strong></p><p>' + e.responseText + '</p>' : e;
        XNAT.dialog.open({
            width: 450,
            title: title,
            content: errormsg,
            buttons: [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true,
                    action: function () {
                        if (closeAll) {
                            xmodal.closeAll();

                        }
                    }
                }
            ]
        });
    }

    /* =============== *
     * Command History *
     * =============== */

    var historyTable, containerHistory, wrapperList;

    XNAT.plugin.containerService.historyTable = historyTable =
        getObject(XNAT.plugin.containerService.historyTable || {});

    XNAT.plugin.containerService.containerHistory = containerHistory =
        getObject(XNAT.plugin.containerService.containerHistory || {});

    function getCommandHistoryUrl(appended) {
        appended = (appended) ? '?' + appended : '';
        return restUrl('/xapi/containers' + appended);
    }
    function getProjectHistoryUrl(projectId, appended) {
        appended = (appended) ? '?' + appended : '';
        return restUrl('/xapi/projects/'+projectId+'/containers'+appended);
    }

    function viewHistoryDialog(e, onclose) {
        e.preventDefault();
        var historyId = $(this).data('id') || $(this).closest('tr').prop('title');
        XNAT.plugin.containerService.historyTable.viewHistory(historyId);
    }

    function sortHistoryData(context) {


        var URL = (context === 'site') ?
            getCommandHistoryUrl() :
            getProjectHistoryUrl(context);

        return XNAT.xhr.getJSON(URL)
            .success(function (data) {
                if (data.length) {
                    // sort data by ID
                    data = data.sort(function (a, b) {
                        return (a.id > b.id) ? 1 : -1
                    });

                    // add a project field before returning. For setup containers, this requires some additional work.
                    var setupContainers = data.filter(function (a) {
                        return (a.subtype) ? a.subtype.toLowerCase() === 'setup' : false
                    });
                    setupContainers.forEach(function (entry) {
                        var projectId = getProjectIdFromMounts(entry);
                        data[entry.id - 1].project = projectId;

                        if (entry['parent-database-id']) {
                            data[entry['parent-database-id'] - 1].project = projectId;
                            data[entry['parent-database-id'] - 1]['setup-container-id'] = entry.id;
                        }
                    });

                    // copy the history listing into an object for individual reference
                    data.forEach(function (historyEntry) {
                        containerHistory[historyEntry.id] = historyEntry;
                    });

                    return data;
                }
            })
    }

    function getProjectIdFromMounts(entry) {
        var mounts = entry.mounts;
        // assume that the first mount of a container is an input from a project. Parse the URI for that mount and return the project ID.
        if (mounts.length) {
            var inputMount = mounts[0]['xnat-host-path'];
            if (inputMount === undefined) return false;

            inputMount = inputMount.replace('/data/xnat/archive/', '');
            inputMount = inputMount.replace('/data/archive/', '');
            inputMount = inputMount.replace('/REST/archive/', '');
            var inputMountEls = inputMount.split('/');
            return inputMountEls[0];
        } else {
            return false;
        }
    }

    function spawnHistoryTable(sortedHistoryObj) {

        var $dataRows = [];

        var styles = {
            command: (200 - 24) + 'px',
            user: (90 - 24) + 'px',
            DATE: (100 - 24) + 'px',
            ROOTELEMENT: (120 - 24) + 'px'
        };
        // var altStyles = {};
        // forOwn(styles, function(name, val){
        //     altStyles[name] = (val * 0.8)
        // });
        return {
            kind: 'table.dataTable',
            name: 'containerHistoryTable',
            id: 'container-history-table',
            // load: URL,
            data: sortedHistoryObj,
            before: {
                filterCss: {
                    tag: 'style|type=text/css',
                    content: '\n' +
                    '#command-history-container td.history-id { width: ' + styles.id + '; } \n' +
                    '#command-history-container td.user .truncate { width: ' + styles.user + '; } \n' +
                    '#command-history-container td.date { width: ' + styles.date + '; } \n' +
                    '#command-history-container tr.filter-timestamp { display: none } \n'
                }
            },
            table: {
                classes: 'highlight hidden',
                on: [
                    ['click', 'a.view-container-history', viewHistoryDialog]
                ]
            },
            trs: function (tr, data) {
                tr.id = data.id;
                addDataAttrs(tr, {filter: '0'});
            },
            sortable: 'command, user, DATE, ROOTELEMENT',
            filter: 'command, user, DATE, ROOTELEMENT',
            items: {
                // by convention, name 'custom' columns with ALL CAPS
                // 'custom' columns do not correspond directly with
                // a data item
                DATE: {
                    label: 'Date',
                    th: {className: 'container-launch'},
                    td: {className: 'container-launch'},
                    filter: function (table) {
                        var MIN = 60 * 1000;
                        var HOUR = MIN * 60;
                        var X8HRS = HOUR * 8;
                        var X24HRS = HOUR * 24;
                        var X7DAYS = X24HRS * 7;
                        var X30DAYS = X24HRS * 30;
                        return spawn('!', [XNAT.ui.select.menu({
                            value: 0,
                            options: {
                                all: {
                                    label: 'All',
                                    value: 0,
                                    selected: true
                                },
                                lastHour: {
                                    label: 'Last Hour',
                                    value: HOUR
                                },
                                last8hours: {
                                    label: 'Last 8 Hrs',
                                    value: X8HRS
                                },
                                last24hours: {
                                    label: 'Last 24 Hrs',
                                    value: X24HRS
                                },
                                lastWeek: {
                                    label: 'Last Week',
                                    value: X7DAYS
                                },
                                last30days: {
                                    label: 'Last 30 days',
                                    value: X30DAYS
                                }
                            },
                            element: {
                                id: 'filter-select-container-timestamp',
                                on: {
                                    change: function () {
                                        var FILTERCLASS = 'filter-timestamp';
                                        var selectedValue = parseInt(this.value, 10);
                                        var currentTime = Date.now();
                                        $dataRows = $dataRows.length ? $dataRows : $$(table).find('tbody').find('tr');
                                        if (selectedValue === 0) {
                                            $dataRows.removeClass(FILTERCLASS);
                                        }
                                        else {
                                            $dataRows.addClass(FILTERCLASS).filter(function () {
                                                var timestamp = this.querySelector('input.container-timestamp');
                                                var containerLaunch = +(timestamp.value);
                                                return selectedValue === containerLaunch - 1 || selectedValue > (currentTime - containerLaunch);
                                            }).removeClass(FILTERCLASS);
                                        }
                                    }
                                }
                            }
                        }).element])
                    },
                    apply: function () {
                        var timestamp = 0, dateString;
                        if (this.history.length > 0) {
                            this.history.forEach(function (h) {
                                if (h['status'] === 'Created') {
                                    timestamp = h['time-recorded'];
                                    dateString = new Date(timestamp);
                                    dateString = dateString.toISOString().replace('T', ' ').replace('Z', ' ').split('.')[0];
                                }
                            });
                        } else {
                            dateString = 'N/A';
                        }
                        return spawn('!', [
                            spawn('span', dateString),
                            spawn('input.hidden.container-timestamp.filtering|type=hidden', {value: timestamp})
                        ])
                    }
                },
                // image: {
                //     label: 'Image',
                //     filter: true, // add filter: true to individual items to add a filter,
                //     apply: function () {
                //         return this['docker-image'];
                //     }
                // },
                command: {
                    label: 'Command',
                    filter: true,
                    apply: function () {
                        var wrapper = XNAT.plugin.containerService.wrapperList[this['wrapper-id']];
                        var label = (wrapper) ?
                            (wrapper.description) ?
                                wrapper.description :
                                wrapper.name
                            : this['command-line'];

                        return spawn('a.view-container-history', {
                            href: '#!',
                            title: 'From image: '+this['docker-image'],
                            data: {'id': this.id},
                            html: label
                        });
                    }
                },
                user: {
                    label: 'User',
                    filter: true,
                    apply: function () {
                        return this['user-id']
                    }
                },
                ROOTELEMENT: {
                    label: 'Root Element',
                    th: {style: { width: '180px' }},
                    filter: true,
                    apply: function(){
                        var rootElements = this.inputs.filter(function(input){ if (input.type === "wrapper-external") return input });
                        if (rootElements.length) {
                            var elementsToDisplay = [];
                            rootElements.forEach(function(element){
                                var label = (element.value.indexOf('scans') >= 0) ?
                                    'session: ' + element.value.split('/')[3] + ' <br>scan: ' + element.value.split('/')[element.value.split('/').length-1] :
                                    element.name + ': ' + element.value.split('/')[element.value.split('/').length-1];

                                var link = (element.value.indexOf('scans') >= 0) ?
                                    element.value.split('/scans')[0] :
                                    element.value;

                                elementsToDisplay.push(
                                    spawn('a.root-element', {
                                        href: XNAT.url.rootUrl('/data/'+link+'?format=html'),
                                        html: label
                                    })
                                );
                            });

                            return spawn('!',elementsToDisplay)
                        }
                        else {
                            return 'Unknown';
                        }
                    }
                }
                // PROJECT: {
                //     label: 'Project',
                //     filter: true,
                //     apply: function () {
                //         var projectId = (this.project) ? this.project : getProjectIdFromMounts(this);
                //         if (projectId) {
                //             return spawn('a', {
                //                 href: rootUrl('/data/projects/' + projectId + '?format=html'),
                //                 html: projectId
                //             });
                //         } else {
                //             return 'Unknown';
                //         }
                //     }
                // }
            }
        }
    }


    historyTable.viewLog = viewLog = function (containerId, logFile) {
        XNAT.xhr.get({
            url: rootUrl('/xapi/containers/' + containerId + '/logs/' + logFile),
            success: function (data) {
                // split the output into lines
                data = data.split('\n');

                XNAT.dialog.open({
                    title: 'View ' + logFile,
                    width: 850,
                    content: null,
                    beforeShow: function (obj) {
                        data.forEach(function (newLine) {
                            obj.$modal.find('.xnat-dialog-content').append(spawn('pre', {'style': {'font-size':'12px','margin':'0', 'white-space':'pre-wrap'}}, newLine));
                        });
                    },
                    buttons: [
                        {
                            label: 'OK',
                            isDefault: true,
                            close: true
                        }
                    ]
                })
            },
            fail: function (e) {
                errorHandler(e, 'Cannot retrieve ' + logFile);
            }
        })
    };

    historyTable.viewHistory = function (id) {
        if (containerHistory[id]) {
            var historyEntry = XNAT.plugin.containerService.containerHistory[id];
            var historyDialogButtons = [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true
                }
            ];

            // build nice-looking history entry table
            var pheTable = XNAT.table({
                className: 'xnat-table compact',
                style: {
                    width: '100%',
                    marginTop: '15px',
                    marginBottom: '15px'
                }
            });

            // add table header row
            pheTable.tr()
                .th({addClass: 'left', html: '<b>Key</b>'})
                .th({addClass: 'left', html: '<b>Value</b>'});

            for (var key in historyEntry) {
                var val = historyEntry[key], formattedVal = '';
                if (Array.isArray(val)) {
                    var items = [];
                    val.forEach(function (item) {
                        if (typeof item === 'object') item = JSON.stringify(item);
                        items.push(spawn('li', [spawn('code', item)]));
                    });
                    formattedVal = spawn('ul', {style: {'list-style-type': 'none', 'padding-left': '0'}}, items);
                } else if (typeof val === 'object') {
                    formattedVal = spawn('code', JSON.stringify(val));
                } else if (!val) {
                    formattedVal = spawn('code', 'false');
                } else {
                    formattedVal = spawn('code', val);
                }

                pheTable.tr()
                    .td('<b>' + key + '</b>')
                    .td([spawn('div', {style: {'word-break': 'break-all', 'max-width': '600px'}}, formattedVal)]);

                // check logs and populate buttons at bottom of modal
                if (key === 'log-paths') {
                    historyDialogButtons.push({
                        label: 'View StdOut.log',
                        close: false,
                        action: function(){
                            var jobid = historyEntry['container-id'];
                            if (!jobid || jobid === "") {
                                jobid = historyEntry['service-id'];
                            }
                            historyTable.viewLog(jobid,'stdout')
                        }
                    });

                    historyDialogButtons.push({
                        label: 'View StdErr.log',
                        close: false,
                        action: function(){
                            var jobid = historyEntry['container-id'];
                            if (!jobid || jobid === "") {
                                jobid = historyEntry['service-id'];
                            }
                            historyTable.viewLog(jobid,'stderr')
                        }
                    })
                }
                if (key === 'setup-container-id') {
                    historyDialogButtons.push({
                        label: 'View Setup Container',
                        close: true,
                        action: function () {
                            historyTable.viewHistory(historyEntry[key]);
                        }
                    })
                }
                if (key === 'parent-database-id' && historyEntry[key]) {
                    var parentId = historyEntry[key];
                    historyDialogButtons.push({
                        label: 'View Parent Container',
                        close: true,
                        action: function () {
                            historyTable.viewHistory(parentId);
                        }
                    })
                }

            }

            // display history
            XNAT.ui.dialog.open({
                title: historyEntry['wrapper-name'],
                width: 800,
                scroll: true,
                content: pheTable.table,
                buttons: historyDialogButtons
            });
        } else {
            console.log(id);
            XNAT.ui.dialog.open({
                content: 'Sorry, could not display this history item.',
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true
                    }
                ]
            });
        }
    };

    historyTable.init = historyTable.refresh = function (context) {
        context = context || 'site';
        wrapperList = getObject(XNAT.plugin.containerService.wrapperList || {});

        var $manager = $('#command-history-container'),
            _historyTable;

        sortHistoryData(context).done(function (data) {
            if (data.length) {
                // sort list of container launches by execution time, descending
                data = data.sort(function (a, b) {
                    return (a.history[0]['time-recorded'] < b.history[0]['time-recorded']) ? 1 : -1
                });

                _historyTable = XNAT.spawner.spawn({
                    historyTable: spawnHistoryTable(data)
                });
                _historyTable.done(function () {
                    var msg = (context === 'site') ?
                        data.length + ' Containers Launched On This Site' :
                        data.length + ' Containers Launched For '+context;
                    $manager.empty().append(
                        spawn('h3', {style: {'margin-bottom': '1em'}}, msg)
                    );
                    this.render($manager, 20);
                });
            }
            else {
                $manager.empty().append('No history entries to display')
            }
        });
    };

}));