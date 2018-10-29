console.log('containerServices-projectSearchLauncher.js');

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
}(function(){
    var projectSearchLauncher;

    XNAT.plugin.containerService.projectSearchLauncher = projectSearchLauncher =
        getObject(XNAT.plugin.containerService.projectSearchLauncher || {});

    /*
     * To create a bulk container launch from a data search table:
     * 1. Get the JSON list of XNAT data IDs to target as your root element, and the root element name
     * 1a. The launcher will be provided with an XNAT search ID to query to get that list
     * 2. Submit the target list to the bulklaunch API for that command & wrapper
     * 3. Figure out the rest of the parameters and inputs for launching using the same logic as the scan bulk launcher
     */

    function findLabel(key){
        return key.indexOf('identifier') > 0;
    }
    function errorHandler(e, title, closeAll){
        console.log(e);
        title = (title) ? 'Error Found: '+ title : 'Error';
        closeAll = (closeAll === undefined) ? true : closeAll;
        var errormsg = (e.statusText) ? '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p><p>' + e.responseText + '</p>' : e;
        XNAT.dialog.open({
            width: 450,
            title: title,
            content: errormsg,
            buttons: [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true,
                    action: function(){
                        if (closeAll) {
                            xmodal.closeAll();

                        }
                    }
                }
            ]
        });
    }
    function queueCount($list){
        var numChecked = $list.find('input[type=checkbox]:checked').not('.selectable-select-all').length;
        $(document).find('#queue-targets').html(numChecked);
    }

    projectSearchLauncher.confirmTargets = function(targetList, config){
        // config contains information necessary to build the container launcher
        // Ask user to confirm the list of targets before building the container launch UI

        if (targetList.length) {

            var s = (targetList.length > 1) ? 's' : '';

            XNAT.dialog.open({
                title: 'Confirm Data To Run',
                width: 500,
                content: spawn('div.targetList.panel'),
                beforeShow: function(obj){
                    var inputArea = obj.$modal.find('.targetList');
                    if (!document.documentMode){
                        inputArea
                            .off('change','input[type=checkbox]')
                            .on('change','input[type=checkbox]', function(){
                                queueCount(inputArea);
                            });
                    } else {
                        inputArea
                            .off('mouseup','.selectable-select-one')
                            .on('mouseup','.selectable-select-one', function(){
                                // HACK: wait for selectable table behavior to process, then check checkbox status
                                window.setTimeout(queueCount,50,inputArea);
                            })
                            .on('mouseup','.selectable-select-all', function(){
                                // HACK: wait for selectable table behavior to process, then check checkbox status
                                window.setTimeout(queueCount,50,inputArea);
                            })
                    }
                    
                    inputArea.append(spawn('!',[
                        spawn('h3', '<b id="queue-targets">' + targetList.length + '</b> '+config['root-element-name']+s+' queued for this container launch.'),
                        spawn('p','Select some or all to launch on, or add filters to your search table.')
                        ]));

                    inputArea.append(selectableTable(targetList));
                    inputArea.find('input[type=checkbox]').prop('checked','checked');
                    inputArea.append(
                        spawn('!',[
                            spawn('input|type=hidden',{ name: 'root-element-name', value: config['root-element-name'] }),
                            spawn('input|type=hidden',{ name: 'wrapper-id', value: config['wrapper-id'] })
                            ]));
                },
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: false,
                        action: function(obj){
                            var targets = [];
                            obj.$modal.find('input.target').each(function(){
                                if ($(this).prop('checked')) targets.push($(this).val());
                            });

                            if (!targets.length) {
                                XNAT.ui.dialog.message('Error: No '+ config['root-element-name']+'s are selected.');
                                return false;
                            } else {
                                var rootElementName = obj.$modal.find('input[name=root-element-name]').val();
                                var wrapperId = obj.$modal.find('input[name=wrapper-id]').val();
                                XNAT.ui.dialog.closeAll();
                                XNAT.plugin.containerService.launcher.bulkLaunchDialog(wrapperId,rootElementName,targets);
                            }
                        }
                    },
                    {
                        label: 'Cancel',
                        close: true
                    }
                ]
            });

        } else {

            xmodal.message('Error: no data selected. Cannot run container.');

        }
    };

    function selectableTable(data){
        var tableHeader = spawn('div.data-table-wrapper.no-body',[
            spawn('table.xnat-table.fixed-header.clean',[
                spawn('thead',[
                    spawn('tr',[
                        spawn('th.toggle-all',{ style: { width: '45px' }},[
                            spawn('input.selectable-select-all|type=checkbox',{ title: 'Toggle All'})
                        ]),
                        spawn('th.left',{ style: { width: '200px' }},'Label'),
                        spawn('th.left',{ style: { width: '213px' }},'XNAT Accession ID')
                    ])
                ])
            ])
        ]);

        var tableBodyRows = [];
        // loop over an array of data, populate the table body rows
        // max table width in a 500-px dialog is 458px
        data.forEach(function(row){
            tableBodyRows.push(
                spawn('tr.selectable-tr',{ id: row['accession-id'] },[
                    spawn('td.table-action-controls.table-selector.center',{ style: { width: '45px' }}, [
                        spawn('input.selectable-select-one.target|type=checkbox', { value: row['accession-id'] })
                    ]),
                    spawn('td',{ style: { width: '200px' }},row['label']),
                    spawn('td',{ style: { width: '213px' }},row['accession-id'])
                ])
            );
        });
        
        var tableBody = spawn('div.data-table-wrapper.no-header',{
            style: {
                'max-height': '300px',
                'overflow-y': 'auto'
            }
        },[
            spawn('table.xnat-table.clean.selectable',[
                spawn('tbody', tableBodyRows )
            ])
        ]);

        return spawn('div.data-table-container',[
            tableHeader,
            tableBody
        ]);
    }

    projectSearchLauncher.open = function(){
        // find obj in the config param of the passed object
        var obj = this.cfg.config.onclick.value.obj;

        XNAT.xhr.getJSON({
            url: XNAT.url.rootUrl('/data/search/'+obj['search-id']),
            success: function(data){
                var targetList = [];
                if (data.ResultSet.Result.length){
                    data.ResultSet.Result.forEach(function(target){
                        // determine the label field -- it differs for each project and data type. Return the first matching value.
                        var labelField = Object.keys(target).filter(findLabel)[0];
                        targetList.push({ 'accession-id': target.key, 'label': target[labelField] });
                    });
                }

                projectSearchLauncher.confirmTargets(targetList, obj);
            },
            fail: function(e){
                if (e.status === '500') {
                    e.responseText = 'Please reload the data table and try again.';
                    errorHandler(e,'Expired search table key');
                } else {
                    errorHandler(e);
                }
            }
        })
    };

    $('.tr-selectable td').on('click',function(){
        $(this).find('input[type=checkbox]').click();
    });


}));