The project in this directory is aimed at comparison and exploration
of solutions for creating the store that supports no-downtime upgrades.

Each proposal needs to implement
`org.keycloak.playground.nodowntimeupgrade.base.storage.VersionedStorage`
interface and pass the testsuite. The primary aim of this exercise is to
obtain ideas on various approaches to the storage solution that provides
0-downtime storage.

The `naive_jackson` solution is here only for illustration purposes on
what needs to be implemented, but is intentionally suboptimal, as its
goal is there to illustrate that a solution exists.

None of the solutions will be optimal in all aspects - performance,
storage demands, 0-downtime. They only need to satisfy all the conditions
from the design, in particular avoiding stop-the-world upon update, at
most logarithmic complexity for searching by ID, and at most linear
complexity for searching by single field.
