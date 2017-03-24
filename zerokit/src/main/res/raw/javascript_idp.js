function getElementByClassName(parentClassName, childClassName){
    var elements = window.document.getElementsByClassName(parentClassName);

    if (elements.length > 0){
        elements = elements[0].getElementsByClassName(childClassName);
        return elements.length > 0 ? elements[0].innerHTML : "";
    } else {
        return "";
    }
}

function callFunction(){
    JSInterfaceHtmlExporter.onGetError(getElementByClassName("container", "lead ng-binding"), getElementByClassName("alert alert-danger", "ng-binding"));
}