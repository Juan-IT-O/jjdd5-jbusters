<#if user??>
    <#if user.userRole == 1 >
        <#include "header-admin.ftlh">
    <#elseif user.userRole == 2>
        <#include "include-user-header.ftlh">
    </#if>
<#else >
    <#include "header.ftlh">
</#if>

<div class="container">
    <div class="jumbotron jumbotron-opacity">

        <#if success??>
            <div class="alert alert-success">
                <strong>${success}<a href="index.html"> Główna Strona</a></strong>
            </div>
        </#if>
        <#if fail??>
            <div class="alert alert-danger">
                <strong>${fail}</strong>
            </div>
        </#if>

        <h2 class="lead">Jeśli Twojego miasta nie ma na liście, poinformuj nas! Postaramy się uzupełnić naszą listę.</h2>
        <br><br>
        <form method="post" action="/send-info">
            <div class="form-group">
                <label for="Select10">Jakiego miasta szukasz ?</label>
                <input name="city" type="text" class="form-control" id="Select10 "
                       pattern="^[a-zA-Z]+(?:[\s-][a-zA-Z]+)*$"
                       oninvalid="this.setCustomValidity('Wpisana wartość jest nieprawidłowa!')"
                       oninput="this.setCustomValidity('')" required>
            </div>

            <div class="form-group">
                <label for="Select11">Jakiej dzielnicy szukasz ?</label>
                <input name="district" type="text" class="form-control" id="Select11"
                       pattern="^[a-zA-Z]+(?:[\s-][a-zA-Z]+)*$"
                       oninvalid="this.setCustomValidity('Wpisana wartość jest nieprawidłowa!')"
                       oninput="this.setCustomValidity('')">
            </div>

            <input class="btn btn-primary" type="submit" value="Wyślij" name="upload" id="upload"/>
        </form>
    </div>
</div>
</div>

<#if user??>
    <#include "include-user-footer.ftlh">
<#else >
    <#include "footer.ftlh">
</#if>