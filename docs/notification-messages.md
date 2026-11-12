# Notification messages

The Community Edition emits no push notifications. `MessageSentListener` extensions registered by other modules are
called after every successful text send and attachment upload (not for tempId duplicates), after a message edit
(with the new text) and after a reaction is added or changed (with the emoji). They are not called for an unchanged
reaction, a reaction removal, a forward or a delete. The message may be older than some members' visibility floor
(`clearedAt`): a listener that notifies room members must not reach members who cannot see that message.
