<html>
    <head><title>FAPI demo</title>
    </head>
    <body onload="submitWithAction('process-fragment')">

    <h2>Code Parser</h2>
    <button onclick="submitWithAction('process-fragment')">Parse fragment</button>

<form id="my-form" method="post" action="${url.action}">
    <input type="hidden" id="authz-response-url" name="authz-response-url">
    <input type="hidden" id="my-action" name="my-action">
</form>

    <script>

    function submitWithAction(myAction) {
        document.getElementById('authz-response-url').value = window.location.href;
        document.getElementById('my-action').value = myAction;
        document.getElementById('my-form').submit();
    }

    </script>
</body>
</html>