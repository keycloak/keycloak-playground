How to store Keycloak objects in filesystem
===========================================

The aim of this playground is to find an efficient way to (de)serialize
an object representation (e.g. realm, client, user), and prove that the
serialized form is both fast to access, searchable by certain attributes,
fast to write.

For example one form might be plain text files in JSON or YAML format
with custom indices.
