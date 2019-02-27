/*
 * web: commandUiLauncher.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Flexible script to be used in the UI to launch 
 */

console.log('commandUiLauncher.js');

var XNAT = getObject(XNAT || {});

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

    var launcher,
        undefined,
        launcherMenu = $('#container-launch-menu'),
        rootUrl = XNAT.url.rootUrl,
        csrfUrl = XNAT.url.csrfUrl,
        projectId = XNAT.data.context.projectID,
        xsiType = XNAT.data.context.xsiType,
        containerMenuItems;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.containerService = 
        getObject(XNAT.plugin.containerService || {});

    XNAT.plugin.containerService.launcher = launcher =
        getObject(XNAT.plugin.containerService.launcher || {});


    function errorHandler(e){
        console.log(e);
        xmodal.alert({
            title: 'Error',
            content: '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p>',
            okAction: function () {
                xmodal.closeAll();
            }
        });
    }
    function uniqueArray(value, index, self) {
        return self.indexOf(value) === index;
    }
    function getEnabledCommandsUrl(appended){
        appended = isDefined(appended) ? appended : '';
        return rootUrl('/xapi/commands' + appended);
    }

    function getCommandUrl(commandId,wrapperName,appended){
        if (!commandId || !wrapperId) return false;
        appended = (appended) ? '/' + appended : '';
        return rootUrl('/xapi/commands/'+commandId+'/wrappers/'+wrapperName+appended);
    }

    function getCommandConfigUrl(wrapperName,commandId,project){
        var projectStr = (project) ? '/projects/' + project : '';
        var commandStr = (commandId) ? '/commands/'+commandId : '';
        return rootUrl('/xapi' + projectStr + commandStr + '/wrappers/' + wrapperName + '/config');
    }
    function getLauncherUI(wrapperId,rootElementName,rootElementValue){
        return rootUrl('/xapi/wrappers/'+wrapperId+'/launch?'+rootElementName+'='+rootElementValue);
    }
    function getProjectLauncherUI(wrapperId,rootElementName,rootElementValue){
        return rootUrl('/xapi/projects/'+projectId+'/wrappers/'+wrapperId+'/launch?'+rootElementName+'='+rootElementValue);
    }
    function containerLaunchUrl(wrapperId){
        return csrfUrl('/xapi/wrappers/'+wrapperId+'/launch');
    }
    function projectContainerLaunchUrl(project,wrapperId){
        return csrfUrl('/xapi/projects/'+project+'/wrappers/'+wrapperId+'/launch');
    }
    function bulkLaunchUrl(wrapperId,rootElements){
        // array of root elements can be provided
        if (rootElements) {
            return csrfUrl('/xapi/wrappers/'+wrapperId+'/bulklaunch?'+rootElements)
        } else {
            return csrfUrl('/xapi/wrappers/'+wrapperId+'/bulklaunch');
        }
    }
    function bulkProjectLaunchUrl(project,wrapperId,rootElements){
        // array of root elements can be provided
        if (rootElements) {
            return csrfUrl('/xapi/projects/'+project+'/wrappers/'+wrapperId+'/bulklaunch?'+rootElements)
        } else {
            return csrfUrl('/xapi/projects/'+project+'/wrappers/'+wrapperId+'/bulklaunch');
        }
    }
    function sessionUrl(){
        var sessionId = (XNAT.data.context.isImageSession) ? XNAT.data.context.ID : null;
        if (!sessionId) return false;
        return rootUrl('/REST/experiments/'+sessionId);
    }
    function fullScanPath(scanId){
        var sessionId = (XNAT.data.context.isImageSession) ? XNAT.data.context.ID : null;
        if (!sessionId) return false;
        return '/archive/experiments/'+sessionId+'/scans/'+scanId;
    }

    /*
     * Launcher UI Builder (i.e. CommandResolver)
     */

    /*
     * Panel form elements for launcher
     */

    var helptext = function(description) {
        return (description) ? '' : spawn('div.description',description);
    };
    var vertSpace = function(condensed) {
        return (condensed) ? '' : spawn('br.clear');
    };

    var defaultConfigInput = function(input){
        var name = input.name || input.label,
            value = input.value,
            label = input.label,
            description = input.description || '',
            required = input.required || false,
            classes = ['panel-input'],
            dataProps = {};
        value = (value === undefined || value === null || value == 'null') ? '' : value;
        label = label || name;
        description = description || '';

        if (input.childInputs) {
            classes.push('has-children');
            dataProps['children'] = input.childInputs.join(',');
        }

        if (required) {
            classes.push('required');
            description += ' (Required)';
        }

        return XNAT.ui.panel.input.text({
            name: name,
            value: value,
            description: description,
            label: label,
            data: dataProps,
            className: classes.join(' ')
        }).element;
    };

    var booleanEval = function(val){
        var trueValues = ['1','y','yes','true','t'];
        return (trueValues.indexOf(val.toString().toLowerCase()) >= 0 );
    };

    var configCheckbox = function(input){
        var name = input.name || input.outerLabel,
            value = input.value,
            checked = input.checked,
            boolean = input.boolean,
            outerLabel = input.outerLabel,
            innerLabel = input.innerLabel,
            description = input.description || '',
            required = input.required || false,
            condensed = input.condensed || false,
            classes = ['panel-element panel-input'],
            dataProps = { name: name },
            disabled = input.disabled || false,
            attr = {};

        if (input.childInputs) {
            classes.push('has-children');
            dataProps['children'] = input.childInputs.join(',');
        }

        if (checked === 'checked') attr['checked'] = 'checked';
        if (disabled) attr['disabled'] = 'disabled';

        value = (boolean) ? 'true' : value;

        if (required) {
            classes.push('required');
            description += ' (Required)';
        }

        return spawn('div', { className: classes.join(' '), data: dataProps }, [
            spawn('label.element-label', outerLabel),
            spawn('div.element-wrapper', [
                spawn('label', [
                    spawn('input', { type: 'checkbox', name: name, value: value, attr: attr }),
                    innerLabel
                ]),
                helptext(description)
            ]),
            vertSpace(condensed)
        ]);
    };

    var configRadio = function(input){
        var name = input.name || input.outerLabel,
            value = input.value,
            checked = input.checked,
            boolean = input.boolean,
            outerLabel = input.outerLabel,
            innerLabel = input.innerLabel,
            description = input.description || '',
            required = input.required || false,
            condensed = input.condensed || false,
            classes = ['panel-element panel-input'],
            dataProps = { name: name },
            disabled = input.disabled || false,
            attr = {};

        if (input.childInputs) {
            classes.push('has-children');
            dataProps['children'] = input.childInputs.join(',');
        }

        if (checked === 'true') attr['checked'] = 'checked';
        if (disabled) attr['disabled'] = 'disabled';

        if (required) {
            classes.push('required');
            description += ' (Required)';
        }

        return spawn('div', { className: classes.join(' '), data: dataProps }, [
            spawn('label.element-label', outerLabel),
            spawn('div.element-wrapper', [
                spawn('label', [
                    spawn('input', { type: 'radio', name: name, value: value, attr: attr }),
                    innerLabel
                ]),
                helptext(description)
            ]),
            vertSpace(condensed)
        ]);
    };

    var configSelect = function(input){
        var name = input.name,
            label = input.label,
            dataProps = input.dataProps || {},
            classes = ['panel-element panel-input'],
            attr = (input.disabled) ? { 'disabled':'disabled'} : {};

        if (input.childInputs) {
            classes.push('has-children');
            dataProps['children'] = input.childInputs.join(',');
        }

        return XNAT.ui.panel.select.single({
            name: name,
            label: label,
            data: dataProps,
            attr: attr,
            className: classes.join(' '),
            options: input.options
        }).element;
    };

    var hiddenConfigInput = function(input) {
        var name = input.name || input.label,
            value = input.value,
            dataProps = {},
            classes = [],
            attr = (input.disabled) ? { 'disabled':'disabled' } : {};

        if (input.childInputs) {
            classes.push('has-children');
            dataProps['children'] = input.childInputs.join(',');
        }

        return XNAT.ui.input.hidden({
            name: name,
            value: value,
            data: dataProps,
            attr: attr
        }).element;
    };

    var staticConfigInput = function(input) {
        var name = input.name || input.label,
            value = input.value,
            valueLabel = input.valueLabel,
            dataProps = { name: name },
            classes = ['panel-input','panel-element'],
            attr = (input.disabled) ? { 'disabled':'disabled' } : {};

        if (input.childInputs) {
            classes.push('has-children');
            dataProps['children'] = input.childInputs.join(',');
        }

        return spawn(
            'div', { className: classes.join(' '), data: dataProps }, [
                spawn('label.element-label', name),
                spawn('div.element-wrapper', { style: { 'word-wrap': 'break-word' } }, valueLabel),
                spawn('input',{
                    type: 'hidden',
                    name: name,
                    value: value,
                    data: dataProps,
                    attr: attr
                }),
                spawn('br.clear')
            ]
        );
    };

    var staticConfigList = function(name,list) {
        var listArray = list.split(',');
        if (listArray.length > 6) {
            return spawn(
                'div.panel-element', { data: { name: name } }, [
                    spawn('label.element-label', name),
                    spawn('div.element-wrapper', [
                        spawn('textarea',{ 'readonly':true, style: { height: '80px' }},listArray.join('\n'))
                    ]),
                    spawn('br.clear')
                ]
            )
        }
        else {
            listArray.forEach(function(item,i){
                listArray[i] = '<li>'+item+'</li>'
            });
            return spawn(
                'div.panel-element', { data: { name: name } }, [
                    spawn('label.element-label', name),
                    spawn('div.element-wrapper', [
                        spawn('ul',{ style: {
                            'list-style-type': 'none',
                            margin: 0,
                            padding: 0
                        }},listArray.join(''))
                    ]),
                    spawn('br.clear')
                ]
            )
        }
    };

    launcher.errorMessages = [];

    launcher.formInputs = function(input) {
        var formPanelElements = [];

        // create a panel.input for each input type
        switch (input.type) {
            case 'scanSelectMany':
                launcher.scanList.forEach(function (scan, i) {
                    var scanOpts = {
                        name: 'scan',
                        value: fullScanPath(scan.id),
                        innerLabel: scan.id + ' - ' + scan['series_description'],
                        condensed: true
                    };
                    if (i === 0) {
                        // first
                        scanOpts.outerLabel = 'scans';
                        formPanelElements.push(configCheckbox(scanOpts));
                    } else if (i < launcher.scanList.length - 1) {
                        // middle
                        formPanelElements.push(configCheckbox(scanOpts));
                    } else {
                        // last
                        scanOpts.condensed = false;
                        formPanelElements.push(configCheckbox(scanOpts));
                    }
                });
                break;
            case 'hidden':
                formPanelElements.push(hiddenConfigInput(input));
                break;
            case 'static':
                formPanelElements.push(staticConfigInput(input));
                break;
            case 'staticList':
                formPanelElements.push(staticConfigList(input.name, input.value));
                break;
            case 'checkbox':
                input.outerLabel = input.label;
                input.innerLabel = input.innerLabel || input.value;
                formPanelElements.push(configCheckbox(input));
                break;
            case 'radio':
                input.outerLabel = input.label;
                input.innerLabel = input.innerLabel || input.value;
                formPanelElements.push(configRadio(input));
                break;
            case 'select-one':
                formPanelElements.push(configSelect(input));
                break;
            case 'boolean':
                input.boolean = true;
                input.outerLabel = input.label;
                input.innerLabel = input.innerLabel || 'True';
                input.checked = (booleanEval(input.value)) ? 'checked' : false;
                formPanelElements.push(configCheckbox(input));
                break;
            default:
                formPanelElements.push(defaultConfigInput(input));
        }

        return formPanelElements;

    };

    launcher.populateForm = function($form, inputList, inputValues, rootElement){
        // receive $form as a jquery form object
        // receives optional input list, which can define just a subgroup of inputs to render.
        // requires input list and default values to be stored in the XNAT.plugin.containerService.launcher object
        // get input values via JSONpath queries

        rootElement = rootElement || false;

        function findInput(inputName, $form){
            return $form.find('input[name='+inputName+']').length || $form.find('select[name='+inputName+']').length || $form.find('textarea[name='+inputName+']').length;
        }
        function setValue(input, newValue, $form){
            newValue = newValue || '';
            switch (input.type) {
                case 'radio' :
                    if (newValue) {
                        var $radioOpt = $form.find('input[name='+input.name+'][value='+newValue+']');
                        try{
                            $radioOpt.prop('checked','checked');
                        }
                        catch(e){
                            console.log('Could not find option with '+newValue+'. ',e);
                        }
                    } else {
                        $form.find('input[name='+input.name+']:checked').prop('checked',false);
                    }
                    break;
                case 'boolean' :
                    if (newValue) {
                        var $radioOpt = $form.find('input[name='+input.name+'][value='+newValue+']');
                        try{
                            $radioOpt.prop('checked','checked');
                        }
                        catch(e){
                            console.log('Could not find option with '+newValue+'. ',e);
                        }
                    } else {
                        $form.find('input[name='+input.name+']:checked').prop('checked',false);
                    }
                    break;
                case 'static' :
                    // newValue should be delivered as an object
                    $form.find('input[name='+input.name+']')
                        .val(newValue.value)
                        .parent('.panel-input').find('.element-wrapper').html(newValue.label);
                    break;
                case 'select-one' :
                    var $select = $form.find('select[name='+input.name+']');
                    if (newValue) {
                        $select.find('option[value='+newValue+']').prop('selected','selected');
                    } else
                    {
                        $select.find('option:selected').prop('selected',false);
                    }
                    break;
                default :
                    // text input
                    $form.find('input[name='+input.name+']').val(newValue);
                    break;
            }
        }

        function renderInput(input, $form, valueArr){
            var selectedVal,
                selectedLabel,
                inputValues = launcher.inputPresets,
                valueArr = valueArr || jsonPath(inputValues, "$..[?(@.name=='"+input.name+"')].values[*]"),
                configInput = extend({}, input);

            configInput.type = (configInput['user-settable'] || configInput.name === rootElement) ? configInput['input-type'] : 'static';

            if (!isArray(valueArr)) {
                // if no values can be set, render the input without a value selected
                selectedVal = '';
                selectedLabel = '';
                // if this is a required input, we might have a problem
                if (configInput.required) {
                    launcher.errorMessages.push('Error: <strong>'+ configInput.label +'</strong> is a required field and has no available values. You may not be able to submit this container.');
                }
            }
            else {
                valueArr = valueArr.filter(uniqueArray);

                // HACK HACK HACK -- The top level element may return improperly-formatted JSON from the jsonPath query.
                if (valueArr[0].values !== undefined) valueArr = valueArr[0].values;

                if (valueArr.length > 1) {
                    if (input['input-type'] === "select-one") {
                        var options = { 'default': { label: 'Select One', attr: { 'selected':'selected'} }};
                        valueArr.forEach(function(val,i){ options['option-'+i] = val });
                        configInput.options = options;

                        configInput.dataProps = { 'childValues': JSON.stringify(valueArr) };
                    }
                    else {
                        // if multiple options exist for an input that isn't designated as a select, treat it as a dependent child
                        selectedVal = '';
                        selectedLabel = '';
                    }
                }
                else {
                    selectedVal = valueArr[0].value;
                    selectedLabel = valueArr[0].label || 'N/A';
                }
            }

            if (findInput(input.name, $form)) {
                // don't render a new input, change the value instead
                var value;
                switch(configInput['input-type']){
                    case 'static':
                        value = {
                            value: selectedVal,
                            label: selectedLabel
                        };
                        break;
                    case 'select-one':
                        // re-render select menu to reflect updated options
                        var thisSelect = $form.find('select[name='+configInput.name+']');
                        thisSelect.empty();
                        for (var opt in configInput.options){
                            var optHtml = '<option ';
                            optHtml += (configInput.options[opt]) ? 'value = "'+configInput.options[opt].value+'"' : '';
                            if (configInput.options[opt].attr) {
                                for (var attr in configInput.options[opt].attr) { optHtml += ' '+attr+'="'+configInput.options[opt].attr[attr]+'"'; }
                            }
                            optHtml += '>'+configInput.options[opt].label+'</option>';
                            thisSelect.append(optHtml);
                        }

                        // also update its child value data object
                        thisSelect.data('childValues',JSON.stringify(valueArr));
                        break;
                    default:
                        value = selectedVal;
                        break;
                }

                setValue(configInput,value,$form);
            }
            else {
                // render a new input
                configInput = extend({
                    value: selectedVal,
                    valueLabel: selectedLabel
                }, configInput); // add value properties without impacting the canonical input list

                if (isArray(jsonPath(valueArr, "$..children[*].name"))) {
                    configInput.childInputs = jsonPath(valueArr, "$..children[*].name")
                        .filter(uniqueArray); // build an array of the input names of all child inputs. Ensure the array is unique.
                }
                $form.append(launcher.formInputs(configInput));
            }


            // iterate recursively over child inputs
            if (isArray(configInput.children) && configInput.children.length > 0) {
                configInput.children.forEach(function(childInput){
                    var childValues = jsonPath(valueArr,"$..[?(@.name == '"+childInput.name+"')].values[*]");
                    if (childValues && childValues.length) {
                        renderInput(childInput, $form, childValues);
                    } else {
                        renderInput(childInput, $form);
                    }
                })
            }
        }

        inputList.forEach(function(thisInput){
            // normalize format of values, only submit those pertinent to this input
            var valueArr;
            if (jsonPath(inputValues, "$..[?(@.name=='"+thisInput.name+"')]")) {
                // inputValues = inputValues.filter(function(valueSet){ return valueSet.name === thisInput.name })[0].values;
                valueArr = jsonPath(inputValues, "$..[?(@.name=='"+thisInput.name+"')].values[*]");
            }
            else {
                valueArr = inputValues;
            }

            renderInput(thisInput, $form, valueArr);
        });
    };

    $(document).on('change','.has-children',function(){
        if ($(this).data('children')) {
            // var children = $(this).data('children').split(',');
            // console.log('Inputs to change: ', children);

            var $form = $(this).parents('.panel'),
                input = $(this).prop('name'),
                selectedVal = $(this).val(),
                children;

            if ($(this).data('childValues')) {
                var valueObj = $(this).data('childValues');
                valueObj = (typeof valueObj === "string") ? JSON.parse(valueObj) : valueObj;
                children = jsonPath(valueObj, "$..[?(@.value=='"+selectedVal+"')].children[*]")
                    .filter(function(value, index, self) { return self.indexOf(value) === index; });
            }
            else {
                children = jsonPath(launcher.inputPresets, "$.[?(@.name=='"+input+"')]..[?(@.value=='"+selectedVal+"')].children[*]")
                    .filter(function(value, index, self) { return self.indexOf(value) === index; });
            }

            children.forEach(function(childVal){
                var childInputList = jsonPath(launcher.inputList, "$..children[?(@.name=='"+childVal.name+"')]");
                launcher.populateForm($form, childInputList, childVal.values);
            })
        }
    });


    /*
     ** Launcher Options
     */

    function launchOneContainer(configData,rootElement,wrapperId){

        var workList = configData['input-config'];
        launcher.inputList = configData['input-config'];
        launcher.inputPresets = configData['input-values'];

        var launcherContent = spawn('div.panel',[
            spawn('p','Please specify settings for this container.'),
            spawn('div.standard-settings'),
            spawn('div.advanced-settings-container.hidden',[
                spawn('div.advanced-settings-toggle'),
                spawn('div.advanced-settings')
            ])
        ]);

        if (workList.filter(function(input){ return input.name === rootElement }).length > 0) {
            // if the root element is specified in the list of inputs ...


            XNAT.ui.dialog.open({
                title: 'Set Container Launch Values',
                content: launcherContent,
                width: 550,
                scroll: true,
                beforeShow: function(obj){
                    launcher.errorMessages = [];
                    xmodal.loading.open({title: 'Configuring Container Launcher'});
                    var $panel = obj.$modal.find('.panel');
                    var $standardInputContainer = $panel.find('.standard-settings');
                    var $advancedInputContainer = $panel.find('.advanced-settings');

                    launcher.populateForm($panel, workList, launcher.inputPresets, rootElement);

                },
                afterShow: function(obj){
                    xmodal.loading.close();
                    if (isArray(launcher.errorMessages) && launcher.errorMessages.length) {
                        var $panel = obj.$modal.find('.panel');
                        launcher.errorMessages.forEach(function(msg){
                            $panel.prepend(spawn('div.warning',{style: { 'margin-bottom': '1em' }},msg));
                        });
                    }
                },
                buttons: [
                    {
                        label: 'Run Container',
                        isDefault: true,
                        close: false,
                        action: function(obj){
                            var $panel = obj.$modal.find('.panel'),
                                targetData = {};

                            // check all inputs for invalid characters
                            var $inputs = $panel.find('input'),
                                runContainer = true;
                            $inputs.each(function(){
                                var input = $(this)[0];
                                if (!launcher.noIllegalChars(input)) {
                                    runContainer = false;
                                    $(this).addClass('invalid');
                                }
                            });

                            if (runContainer) {
                                // gather form input values
                                targetData[rootElement] = $panel.find('input[name='+rootElement+']').val();

                                $panel.find('input').not(':disabled').not('[type=checkbox]').not('[type=radio]').not('[name='+rootElement+']').each(function(){
                                    // get the name and value from each text element and add it to our data to post
                                    var key = $(this).prop('name');
                                    targetData[key] = $(this).val();
                                });

                                $panel.find('input[type=checkbox]').not(':disabled').each(function(){
                                    var key = $(this).prop('name');
                                    var val = ($(this).is(':checked')) ? $(this).val() : false;
                                    targetData[key] = val;
                                });

                                $panel.find('select').not(':disabled').each(function(){
                                    var key = $(this).prop('name');
                                    var val = $(this).find('option:selected').val();
                                    targetData[key] = val;
                                });

                                var dataToPost = targetData;

                                xmodal.loading.open({ title: 'Launching Container...' });

                                var projectContext = XNAT.data.context.project;
                                var launchUrl = (projectContext.length) ?
                                    projectContainerLaunchUrl(projectContext,wrapperId) :
                                    containerLaunchUrl(wrapperId);

                                XNAT.xhr.postJSON({
                                    url: launchUrl,
                                    data: JSON.stringify(dataToPost),
                                    success: function(data){
                                        xmodal.loading.close();

                                        var messageContent;
                                        if (data.status === 'success') {
                                            if ( data['type'] === 'service') {
                                                messageContent = spawn('p',{ style: { 'word-wrap': 'break-word'}}, 'Service ID: '+data['service-id']);
                                            } else {
                                                messageContent = spawn('p',{ style: { 'word-wrap': 'break-word'}}, 'Container ID: '+data['container-id']);
                                            }
                                        }else {
                                            messageContent = spawn('p', data.message);
                                        }

                                        XNAT.ui.dialog.open({
                                            title: 'Container Launch <span style="text-transform: capitalize">'+data.status+'</span>',
                                            content: messageContent,
                                            buttons: [
                                                {
                                                    label: 'OK',
                                                    isDefault: true,
                                                    close: true,
                                                    action: XNAT.ui.dialog.closeAll()
                                                }
                                            ]
                                        });
                                    },
                                    fail: function (e) {
                                        xmodal.loading.close();

                                        if (e.responseJSON.message) {
                                            var data = e.responseJSON;
                                            var messageContent = spawn('div',[
                                                spawn('p',{ style: { 'font-weight': 'bold' }}, 'Error Message:'),
                                                spawn('pre.json', data.message),
                                                spawn('p',{ style: { 'font-weight': 'bold' }}, 'Parameters Submitted To XNAT:'),
                                                spawn('div', prettifyJSON(data.params))
                                            ]);

                                            XNAT.ui.dialog.open({
                                                title: 'Container Launch <span style="text-transform: capitalize">'+data.status+'</span>',
                                                content: messageContent,
                                                buttons: [
                                                    {
                                                        label: 'OK',
                                                        isDefault: true,
                                                        close: true,
                                                        action: XNAT.ui.dialog.closeAll()
                                                    }
                                                ]
                                            });
                                        } else {
                                            errorHandler(e);
                                        }
                                    }
                                });

                            } else {
                                // don't run container if invalid characters are found
                                XNAT.dialog.open({
                                    title: 'Cannot Launch Container',
                                    content: 'Illegal characters were found in your inputs. Please correct this and try again.',
                                    width: 400,
                                    buttons: [
                                        {
                                            label: 'OK',
                                            isDefault: true,
                                            close: true
                                        }
                                    ]
                                });
                                return false;
                            }

                        }
                    },
                    {
                        label: 'Cancel',
                        isDefault: false,
                        close: true
                    }
                ]
            });

        } else {
            errorHandler({
                responseText: 'Could not launch command. Root element "'+rootElement+'" not found in the list of inputs provided.'
            });
        }
    }

    function launchManyContainers(configData,rootElement,wrapperId,targets,targetLabels,project){
        /* In a bulk launcher, a list of input objects will be passed to the launcher.
         * The launcher should consider the target elements to be static
         * (i.e. once selected and sent to the bulk launcher, the user shouldn't be re-selecting them)
         * Also, any child elements of the root element should also be set to their default values.
         * Users should be able to set other inputs in bulk for all selected root elements
         * If there are child elements of non-root inputs, they will be treated as standard inputs so they can be bulk-settable
         * After the user makes their selections, a bulk object is assembled from the inputs and sent to the bulk launcher
         */

        var workList = configData['input-config'];
        launcher.inputList = configData['input-config'];

        var launcherContent = spawn('div.panel',[
            spawn('p','Please specify settings for this container.'),
            spawn('div.target-list')
        ]);

        if ( jsonPath(workList, "$..name").indexOf(rootElement) >=0 ) { // if the specified root element matches an input parameter, we can proceed

            XNAT.ui.dialog.open({
                title: 'Set Container Launch Values',
                content: launcherContent,
                width: 550,
                scroll: true,
                beforeShow: function(obj){
                    launcher.errorMessages = [];

                    var $panel = obj.$modal.find('.panel'),
                        $targetListContainer = $panel.find('.target-list');

                    // display root elements first
                    $targetListContainer.append(spawn('p',[ spawn('strong', targets.length + ' item(s) selected to run in bulk.' )]));

                    var targetList = launcher.formInputs({ name: rootElement, type: 'staticList', value: targetLabels.toString() });
                    $targetListContainer.append(targetList);

                    // loop through each input and determine how to display it
                    // root element -- create hidden inputs
                    // child element of root element -- create hidden inputs
                    // standard inputs with no children -- append the appropriate UI element
                    // standard inputs with children -- append the UI element and the child element(s) in a child element wrapper
                    // advanced inputs (that aren't children) -- append the UI element to the advanced input container

                    targets.forEach(function(target,k){
                        // iterate through each list of targets.
                        // reset the stored preset values for each iteration
                        launcher.inputPresets = configData['input-values'][k];

                        if (k===0) {
                            // on first iteration, create all user-settable inputs

                            $panel.append(spawn('div',{ className: 'bulk-master bulk-inputs inputs-'+k },[
                                spawn('div.standard-settings'),
                                spawn('div.advanced-settings-container.hidden',[
                                    spawn('div.advanced-settings-toggle'),
                                    spawn('div.advanced-settings')
                                ])
                            ]));

                            var $standardInputContainer = $panel.find('.standard-settings'),
                                $advancedInputContainer = $panel.find('.advanced-settings');

                            // hide the root element since it has been displayed in the target list
                            workList.forEach(function(input){ if (input.name === rootElement) input['input-type'] = 'hidden' });

                            launcher.populateForm($standardInputContainer, workList, launcher.inputPresets, rootElement);

                        } else {
                            // on 2nd - nth inputs, simply create hidden inputs whose values will be toggled by user changes to first set of inputs
                            $panel.append(spawn('div',{ className: 'hidden bulk-controls bulk-inputs inputs-'+k }));
                            var $bulkInputContainer = $panel.find('.inputs-'+k);

                            var bulkInputs = [].concat(workList);
                            bulkInputs.forEach(function(bulkInput){ bulkInput['input-type'] = 'hidden' });

                            launcher.populateForm($bulkInputContainer, bulkInputs, launcher.inputPresets, rootElement);

                        }

                    });

                },
                afterShow: function(obj){
                    xmodal.loading.close();
                    if (isArray(launcher.errorMessages) && launcher.errorMessages.length) {
                        var $panel = obj.$modal.find('.panel');
                        launcher.errorMessages.forEach(function(msg){
                            $panel.prepend(spawn('div.warning',{style: { 'margin-bottom': '1em' }},msg));
                        });
                    }
                },
                buttons: [
                    {
                        label: 'Run Container(s)',
                        isDefault: true,
                        close: false,
                        action: function(obj){
                            var $panel = obj.$modal.find('.panel'),
                                bulkData = [];

                            // check all inputs for invalid characters
                            var $inputs = $panel.find('input'),
                                runContainer = true;
                            $inputs.each(function(){
                                var input = $(this)[0];
                                if (!launcher.noIllegalChars(input)) {
                                    runContainer = false;
                                    $(this).addClass('invalid');
                                }
                            });

                            if (runContainer) {
                                $panel.find('.bulk-inputs').each(function(){
                                    // iterate over each set of inputs and add an object of inputs and values to the bulkData array
                                    var targetData = {},
                                        $thisPanel = $(this);

                                    // gather form input values
                                    targetData[rootElement] = $thisPanel.find('input[name='+rootElement+']').val();

                                    $thisPanel.find('input').not(':disabled').not('[type=checkbox]').not('[type=radio]').not('[name='+rootElement+']').each(function(){
                                        // get the name and value from each text element and add it to our data to post
                                        var key = $(this).prop('name');
                                        targetData[key] = $(this).val();
                                    });

                                    $thisPanel.find('input[type=checkbox]').not(':disabled').each(function(){
                                        var key = $(this).prop('name');
                                        var val = ($(this).is(':checked')) ? $(this).val() : false;
                                        targetData[key] = val;
                                    });

                                    $thisPanel.find('select').not(':disabled').each(function(){
                                        var key = $(this).prop('name');
                                        var val = $(this).find('option:selected').val();
                                        targetData[key] = val;
                                    });

                                    bulkData.push(targetData);
                                });

                                var dataToPost = bulkData;
                                var launchUrl = (project) ?
                                    bulkProjectLaunchUrl(project,wrapperId) :
                                    bulkLaunchUrl(wrapperId);

                                XNAT.xhr.postJSON({
                                    beforeSend: function() {
                                        XNAT.ui.dialog.closeAll();
                                        XNAT.ui.dialog.alert("Containers are being launched in the background. " +
                                            "You may continue to work, refreshing the dashboard to see updated progress.");
                                        return true;
                                    },
                                    url: launchUrl,
                                    data: JSON.stringify(dataToPost),
                                    success: function(data){
                                        // bulk launch success returns two arrays -- containers that successfully launched, and containers that failed to launch
                                        var messageContent = [],
                                            totalLaunchAttempts = data.successes.concat(data.failures).length;
                                        if (data.failures.length > 0) {
                                            messageContent.push( spawn('div.message',data.successes.length + ' of '+totalLaunchAttempts+' containers successfully launched.') );
                                        } else if(data.successes.length > 0) {
                                            messageContent.push( spawn('div.success','All containers successfully launched.') );
                                        } else {
                                            errorHandler({
                                                statusText: 'Something went wrong. No containers were launched.'
                                            });
                                        }

                                        if (data.successes.length > 0) {
                                            messageContent.push( spawn('h3',{'style': {'margin-top': '2em' }},'Successful Container Launches') );

                                            data.successes.forEach(function(success){
                                                if (success['type'] === 'service') {
                                                    messageContent.push( spawn('p',[spawn('strong','Service ID: '),spawn('span',success['service-id']) ]));
                                                } else {
                                                    messageContent.push( spawn('p',[
                                                        spawn('strong','Container ID: '),
                                                        spawn('span',success['container-id'])
                                                    ]) );
                                                }
                                                messageContent.push( spawn('div',prettifyJSON(success.params)) );
                                            });
                                        }

                                        if (data.failures.length > 0){
                                            messageContent.push( spawn('h3',{'style': {'margin-top': '2em' }},'Failed Container Launches') );
                                            data.failures.forEach(function(failure){
                                                messageContent.push( spawn('p',{ style: { 'font-weight': 'bold' }}, 'Error Message:') );
                                                messageContent.push( spawn('pre.json', failure.message) );
                                                messageContent.push( spawn('div',prettifyJSON(failure.params)) );
                                            });
                                        }

                                        XNAT.ui.dialog.open({
                                            title: 'Container Launch Success',
                                            content: spawn('div', messageContent ),
                                            buttons: [
                                                {
                                                    label: 'OK',
                                                    isDefault: true,
                                                    close: true,
                                                    action: XNAT.ui.dialog.closeAll()
                                                }
                                            ]
                                        });
                                    },
                                    fail: function (e) {
                                        if (e.responseJSON.message) {
                                            var data = e.responseJSON;
                                            var messageContent = spawn('div',[
                                                spawn('p',{ style: { 'font-weight': 'bold' }}, 'Error Message:'),
                                                spawn('pre.json', data.message),
                                                spawn('p',{ style: { 'font-weight': 'bold' }}, 'Parameters Submitted To XNAT:'),
                                                spawn('div', prettifyJSON(data.params))
                                            ]);

                                            XNAT.ui.dialog.open({
                                                title: 'Container Launch <span style="text-transform: capitalize">'+data.status+'</span>',
                                                content: messageContent,
                                                buttons: [
                                                    {
                                                        label: 'OK',
                                                        isDefault: true,
                                                        close: true,
                                                        action: XNAT.ui.dialog.closeAll()
                                                    }
                                                ]
                                            });
                                        } else {
                                            errorHandler(e);
                                        }
                                    }
                                });
                            } else {
                                // don't run container if invalid characters are found
                                XNAT.dialog.open({
                                    title: 'Cannot Launch Container',
                                    content: 'Illegal characters were found in your inputs. Please correct this and try again.',
                                    width: 400,
                                    buttons: [
                                        {
                                            label: 'OK',
                                            isDefault: true,
                                            close: true
                                        }
                                    ]
                                });
                                return false;
                            }
                        }
                    },
                    {
                        label: 'Cancel',
                        isDefault: false,
                        close: true
                    }
                ]
            });


        } else {
            errorHandler({
                statusText: 'Root element mismatch',
                responseText: 'No instance of '+rootElement+' was found in the list of inputs for this command'
            });
        }
    }

    // for bulk launching, apply any user-updated value to all matching inputs
    $(document).on('change','.bulk-master input',function(){
        var name = $(this).prop('name');
        if ($(this).prop('type') === 'checkbox' && !$(this).is(':checked')) {
            $('.bulk-controls').find('input[name='+name+']').val('false');
        } else {
            var changedVal = $(this).val();
            $('.bulk-controls').find('input[name='+name+']').val(changedVal);
        }
    });

    /* ---- Launcher Types ---- */

    launcher.defaultLauncher = function(wrapperId,rootElement,rootElementValue){
        rootElementValue = rootElementValue || XNAT.data.context.ID; // if no value is provided, assume that the current page context provides the value.

        if (!rootElementValue) {
            errorHandler({ responseText: 'Could not launch UI. No value provided for '+rootElement+'.' });
            return false;
        }

        xmodal.loading.open({ title: 'Configuring Container Launcher' });
        var launchUrl = (projectId) ? getProjectLauncherUI(wrapperId,rootElement,rootElementValue) : getLauncherUI(wrapperId,rootElement,rootElementValue);

        XNAT.xhr.getJSON({
            url: launchUrl,
            fail: function(e){
                xmodal.loading.close();
                errorHandler({
                    statusText: e.statusText,
                    responseText: 'Could not launch UI with value: "'+rootElementValue+'" for root element: "'+rootElement+'".'
                });
            },
            success: function(data){
                xmodal.loading.close();
                launchOneContainer(data,rootElement,wrapperId);
            }
        })
    };

    launcher.singleScanDialog = function(wrapperId,rootElementPath){
        // end goal is submitting to /xapi/commands/launch/
        // need to build UI with input values from /xapi/wrappers/{id}/launchui, specifying the root element name and path

        var launchUrl = (projectId) ? getProjectLauncherUI(wrapperId,'scan',rootElementPath) : getLauncherUI(wrapperId, 'scan', rootElementPath);

        XNAT.xhr.getJSON({
            url: launchUrl,
            fail: function(e){
                errorHandler(e);
            },
            success: function(data){
                var rootElement = 'scan';
                launchOneContainer(data,rootElement,wrapperId);
            }
        });
    };

    launcher.bulkLaunchDialog = function(wrapperId,rootElement,targets,targetLabels,project,commandId){
        // 'targets' should be formatted as a one-dimensional array of XNAT data values (i.e. scan IDs) that a container will run on in series.
        // the 'root element' should match one of the inputs in the command config object, and overwrite it with the values provided in the 'targets' array

        if (projectId.length && !project) project = projectId;

        if (!targets || targets.length === 0) return false;
        if (!targetLabels || targetLabels.length !== targets.length) targetLabels = targets;

        //TODO speed up the serialization of XNAT objects during command preresolution so we can use the bulklaunch UI generator
        // var targetObj = rootElement + '=' + targets.toString();
        // var launchUrl = (project) ?
        //     rootUrl('/xapi/projects/'+project+'/wrappers/'+wrapperId+'/bulklaunch?'+targetObj) :
        //     rootUrl('/xapi/wrappers/'+wrapperId+'/bulklaunch?'+targetObj);
        var launchUrl = getCommandConfigUrl(wrapperId,commandId,project);

        xmodal.loading.open({ title: 'Configuring Container Launcher' });
        XNAT.xhr.getJSON({
            url: launchUrl,
            fail: function(e){
                xmodal.loading.close();
                errorHandler({
                    statusText: e.statusText,
                    responseText: 'Could not launch UI with value(s): "'+targets.toString()+'" for root element: "'+rootElement+'".'
                });
            },
            success: function(data){
                xmodal.loading.close();
                var configData = convertInputstoConfigData(data.inputs, targets, targetLabels, rootElement);
                launchManyContainers(configData,rootElement,wrapperId,targets,targetLabels,project);
            }
        });
    };

    var convertInputstoConfigData = function(inputs, targets, targetLabels, rootElement) {
        // TODO, speed up command pre-resolution to get rid of this nasty method
        //inputs = {"inputs":{"session":{"description":"Input session","type":"Session","default-value":null,"matcher":null,"user-settable":null,"advanced":false,"required":null},"command":{"description":"The command to run","type":"string","default-value":"echo hello world","matcher":null,"user-settable":true,"advanced":false,"required":true},"output-file":{"description":"Name of the file to collect stdout","type":"string","default-value":"out.txt","matcher":null,"user-settable":true,"advanced":false,"required":false}},"outputs":{"output-resource":{"type":"Resource","label":"DEBUG_OUTPUT"}}}
        //configData = "{"meta":{"command-id":12,"command-name":"debug","command-label":"debug","command-description":"Runs a user-provided command","wrapper-id":47,"wrapper-name":"debug-session","wrapper-description":"Run the debug container with a session mounted","image-name":"xnat/debug-command:latest","image-type":"docker"},"input-config":[{"name":"session","label":"session","advanced":false,"required":true,"user-settable":true,"input-type":"static","children":[]},{"name":"command","label":"command","description":"The command to run","advanced":false,"required":true,"user-settable":true,"input-type":"text","children":[]},{"name":"output-file","label":"output-file","description":"Name of the file to collect stdout","advanced":false,"required":false,"user-settable":true,"input-type":"text","children":[]}],"input-values":[[{"name":"session","values":[{"value":"/archive/experiments/LOCAL02_E00001","label":"ses-0","children":[]}]},{"name":"command","values":[{"value":"echo hello world","label":"echo hello world","children":[]}]},{"name":"output-file","values":[{"value":"out.txt","label":"out.txt","children":[]}]}],[{"name":"session","values":[{"value":"/archive/experiments/LOCAL02_E00002","label":"ses-1","children":[]}]},{"name":"command","values":[{"value":"echo hello world","label":"echo hello world","children":[]}]},{"name":"output-file","values":[{"value":"out.txt","label":"out.txt","children":[]}]}]]}"

        var configData = {'input-config': [], 'input-values': []};
        var defaults = [];
        var i = 0;
        var rootInd = -1;
        configData['input-config'] = $.map(inputs, function(value, key){
            value['name'] = key;
            value['children'] = [];
            // See LaunchUi.java > convertResolvedInputTreeToLaunchUiInputTree
            // We don't handle select (seems to only occur for derived inputs?)
            if (!value['user-settable']) {
                value['input-type'] = 'static';
            } else if (value['type'] === 'boolean') {
                value['input-type'] = 'boolean';
            } else {
                value['input-type'] = 'text';
            }
            defaults.push({'name': key, 'values': [{'value': value['default-value'],
                    'label': value['default-value'], 'children':[]}]});
            if (key === rootElement) {
                rootInd = i;
            }
            i++;
            return value;
        });

        //Expects one array of defaults per target
        for (i = 0; i < targets.length; i++) {
            defaults[rootInd]['values'][0]['value'] = targets[i];
            defaults[rootInd]['values'][0]['label'] = targetLabels[i];
            configData['input-values'].push(defaults);
        }
        return configData;
    };

    launcher.noIllegalChars = function(input,exception){
        // examine the to-be-submitted value of an input against a list of disallowed characters and return false if any are found.
        // if an input needs to allow one of these strings, an exception can be passed to this function
        exception = exception || null;
        var illegalCharset = [';', '\\|\\|', '&&', '\\(', '`' ],
            value = input.value,
            pass = true;

        illegalCharset.forEach(function(test){
            if (value.match(test) && test !== exception) {
                pass = false;
            }
        });

        return pass;
    };

    /*
     * Build UI for menu selection
     */

    launcher.containerMenuItems = containerMenuItems = [
        {
            text: 'Run Containers',
            url: '#run',
            submenu: {
                id: 'containerSubmenuItems',
                itemdata: [
                ]
            }
        }
    ];

    launcher.addMenuItem = function(command,commandSet){
        commandSet = commandSet || [];
        var label = command['wrapper-name'];
        if (command['wrapper-description']) if (command['wrapper-description'].length) label = command['wrapper-description'];
        if (command['wrapper-label']) if (command['wrapper-label'].length) label = command['wrapper-label'];

        if (command.enabled){
            commandSet.push(
                spawn('li', [
                    spawn('a', {
                        html: label,
                        href: '#!',
                        className: 'commandLauncher',
                        data: {
                            wrapperid: command['wrapper-id'],
                            rootElementName: command['root-element-name'],
                            uri: command['uri'],
                            launcher: command['launcher']
                        }
                    })
                ]));
        }
        return commandSet;
    };

    launcher.createMenu = function(target,commandSet){
        /*
        var containerMenu = spawn('li.has-submenu',[
            spawn(['a',{ href: '#!', html: 'Run' }]),
            spawn(['ul.dropdown-submenu', itemSet ])
        ]);
        */

        target.append(commandSet);
    };

    /* to be replaced when we kill YUI */
    launcher.addYUIMenuItem = function(command){
        if (command.enabled) {
            var launcher = command.launcher || "default";
            var label = command['wrapper-name'];
            if (command['wrapper-description']) if (command['wrapper-description'].length) label = command['wrapper-description'];
            if (command['wrapper-label']) if (command['wrapper-label'].length) label = command['wrapper-label'];

            containerMenuItems[0].submenu.itemdata.push({
                text: label,
                url: 'javascript:openCommandLauncher({ wrapperid:"'+command['wrapper-id']+'", launcher: "'+launcher+'", rootElement: "'+ command['root-element-name'] + '" })',
                classname: 'enabled wrapped' // injects a custom classname onto the surrounding li element.
            });
        }
    };

    launcher.createYUIMenu = function(target){
        target = target || 'actionsMenu';
        var containerMenu = new YAHOO.widget.Menu('containerMenu', { autosubmenudisplay:true, scrollincrement:5, position:'static' });
        containerMenu.addItems(containerMenuItems);
        if (containerMenuItems[0].submenu.itemdata.length > 0) {
            containerMenu.render(target);
        }
    };

    launcher.init = function() {
        // populate or hide the command launcher based on what's in context
        XNAT.xhr.getJSON({
            url: rootUrl('/xapi/commands/available?project=' + projectId + '&xsiType=' + xsiType),
            success: function (data) {
                var availableCommands = data;
                if (!availableCommands.length) {
                    return false;
                } else {
                    var spawnedCommands = [];
                    availableCommands.forEach(function (command) {
                        launcher.addYUIMenuItem(command);
                    });
                    launcher.createYUIMenu('actionsMenu',spawnedCommands);
                }

            },
            fail: function (e) {
                errorHandler(e);
            }
        });

        // Special case: If this is a session, run a second context check for scans
        // only support scan-level actions if the new scan table is found. 
        if (XNAT.data.context.isImageSession && document.getElementById('selectable-table-scans')) {
            var xsiScanType = xsiType.replace('Session','Scan');

            XNAT.xhr.getJSON({
                url: rootUrl('/xapi/commands/available?project=' + projectId + '&xsiType=' + xsiScanType),
                success: function (data) {
                    var availableCommands = data;
                    if (!availableCommands.length) {
                        return false;
                    } else {
                        // build menu of commands
                        var spawnedCommands = [];
                        availableCommands.forEach(function (command) {
                            command.launcher = 'multiple-scans';
                            command.uri = '';
                            launcher.addMenuItem(command,spawnedCommands);
                        });

                        // add action menu to each scan listing
                        launcher.scanList = XNAT.data.context.scans || [];
                        launcher.scanList.forEach(function(scan){
                            var scanCommands = [];
                            availableCommands.forEach(function (command) {
                                command.launcher = 'single-scan';
                                command.uri = fullScanPath(scan['id']);
                                launcher.addMenuItem(command,scanCommands);
                            });

                            if (scanCommands.length > 0){
                                var scanActionTarget = $('tr#scan-'+scan['id']).find('.single-scan-actions-menu');
                                scanActionTarget.append(scanCommands)
                                $('.run-menu').show(); 
                            }
                        });

                        if (spawnedCommands.length > 0) {
                            // add commands to Bulk Run action menu at the top of the scan table
                            var menuTarget = $('#scanActionsMenu');
                            launcher.createMenu(menuTarget,spawnedCommands);
                            $('.scan-actions-controls').show();
                            $('#scanTable-run-containers').removeClass('hidden');
                        }
                    }
                },
                fail: function(e) {
                    errorHandler(e);
                }
            });
        }
        
    };

    launcher.open = window.openCommandLauncher = function(obj){
        var launcher = obj.launcher,
            wrapperId = obj.wrapperid,
            rootElement = obj.rootElement,
            rootElementValue = obj.rootElementValue || undefined;

        switch(launcher) {
            case 'select-scan':
                XNAT.dialog.message({ title: 'Method Not Supported', content: 'Sorry, the "select-scan" method of launching multiple container is no longer supported. Use the "multiple-scans" method instead.' });
                break;
            case 'single-scan':
                var rootElementPath = obj.uri;
                XNAT.plugin.containerService.launcher.singleScanDialog(wrapperId,rootElementPath);
                break;
            case 'multiple-scans':
                var listOfScanIds = [];
                $('.selectable').find('tbody').find('input:checked').each(function(){
                    var scanId = $(this).val();
                    var scanPath = fullScanPath(scanId);
                    listOfScanIds.push(scanPath);
                });
                XNAT.plugin.containerService.launcher.bulkLaunchDialog(wrapperId,'scan',listOfScanIds);
                break;
            default:
                XNAT.plugin.containerService.launcher.defaultLauncher(wrapperId,rootElement,rootElementValue);
        }
    };

    $(document).on('click','.commandLauncher',function(){
        var launcherObj = $(this).data();
        launcher.open(launcherObj);
    });

    $(document).on('click','.advanced-settings-toggle',function(){
        var advancedPanel = $(this).parent('.advanced-settings-container').find('.advanced-settings');
        if ($(this).hasClass('active')) {
            $(this).removeClass('active');
            advancedPanel.slideUp(300);
        } else {
            $(this).addClass('active');
            advancedPanel.slideDown(300);
        }
    });

    launcher.refresh = function(){
        launcherMenu.html('');
        launcher.init();
    };

    $(document).ready(function(){
        launcher.init();
    });

}));
