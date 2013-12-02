<h2>Message Forwarding</h2>
<form action="SettingsAck_p.html" method="post" enctype="multipart/form-data">
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
        <input name="msgForwardingCmd" id="fwd_cmd" type="text" size="32" value="#[msgForwardingCmd]#" />
        <p>
          The command-line program that should be used to forward the message.
          E.g.: <code>/usr/sbin/sendmail</code>.
        </p>
      </div>
    </fieldset>
    <fieldset class="formSection">
      <legend>Forwarding To</legend>
      <div class="content">
        <input name="msgForwardingTo" id="fwd_to" type="text" maxlength="128" value="#[msgForwardingTo]#" />
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
    <div class="content">
      <button type="submit" name="msgForwarding" class="primary icon-save">Save configuration</button>
    </div>
  </fieldset>
</fieldset>
</form>