# Notification messages

The Community Edition emits no push notifications. `MessageSentListener` extensions registered by other modules are
called after every successful text send and attachment upload (not for tempId duplicates).
