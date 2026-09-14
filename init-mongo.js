// Creates the application user openFHIR connects with.
db = db.getSiblingDB('openfhir');
db.createUser({
  user: 'openfhir',
  pwd: 'openfhir',
  roles: [{ role: 'readWrite', db: 'openfhir' }],
});
