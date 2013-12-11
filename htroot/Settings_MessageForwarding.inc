<div id="devInfo" data-done="html5 validators">
</div>

<h2>Message Forwarding</h2>
<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data" id="settingsMessageForwarding">
  <fieldset>
    <legend>Configuration</legend>
    <div class="description">
      <p>
        With this settings you can activate or deactivate forwarding of YaCy-messages via email.
      </p>
    </div>
    <fieldset class="formSection">
      <legend>Enable / Disable</legend>
      <div class="content">
        <input type="checkbox" name="msgForwardingEnabled" id="msg_forwarding" #(msgForwardingEnabled)#::checked="checked" #(/msgForwardingEnabled)#/>
        <label for="msg_forwarding">Enable message forwarding via email.</label>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Forwarding Command</legend>
      <div class="content">
        <input name="msgForwardingCmd" id="fwdCmd" type="text" size="32" value="#[msgForwardingCmd]#" />
        <div class="formHint hidden">
          <p class="error" data-id="empty">
            Please enter command.
          </p>
        </div>
        <p>
          The command-line program that should be used to forward the message.
          E.g.: <code>/usr/sbin/sendmail</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Forwarding To</legend>
      <div class="content">
        <input name="msgForwardingTo" id="fwdTo" type="text" maxlength="128" value="#[msgForwardingTo]#" />
        <div class="formHint hidden">
          <p class="error" data-id="empty">
            Please enter a forwarding target.
          </p>
        </div>
        <p>
         The recipient email-address.
         E.g.: <code>root@localhost</code>.
       </p>
     </div>
   </fieldset>
   <fieldset class="formControl">
    <div class="description">
      <p>
        <s class="sym sym-hint"></s>
        Changes will take effect immediately.
      </p>
    </div>
    <div id="ycu-error-count" class="description hiddden"></div>
    <div class="content">
      <button type="submit" id="submitSettings" name="msgForwarding" class="primary icon-save">Save configuration</button>
    </div>
  </fieldset>
</fieldset>
</form>