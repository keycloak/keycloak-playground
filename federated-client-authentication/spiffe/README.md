# SPIFFE demos for Keycloak

Choose a workflow:

- [Simple demo](simple/README.md): one local SPIRE server and agent.
- [Federated demo](federated/README.md): two isolated k3d clusters with independent SPIRE trust domains.

[SPIFFE](https://spiffe.io/docs/latest/spiffe-about/overview/) defines workload identities and SVIDs. The [SPIFFE Federation specification](https://spiffe.io/docs/latest/spiffe-specs/spiffe_federation/) describes trust-bundle exchange between domains. These demos use [SPIRE](https://spiffe.io/docs/latest/spire-about/spire-concepts/), an implementation of SPIFFE.
