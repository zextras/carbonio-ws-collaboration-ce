# Notification messages

The Community Edition emits no push notifications. `MessageSentListener` extensions registered by other modules are
called after every successful text send and attachment upload (not for tempId duplicates), after a message edit
(with the new text) and after every reaction set (added, changed or set again with the same value; with the emoji). They
are not called for a reaction removal, a forward or a delete. The message may be older than some members' visibility floor
(`clearedAt`): a listener that notifies room members must not reach members who cannot see that message.
