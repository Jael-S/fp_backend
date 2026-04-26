/*
  Corrige inconsistencia historica:
  - Departamento.responsableId definido
  - Usuario.departamentoId vacio o distinto
*/

db.departamentos
  .find({ responsableId: { $ne: null } })
  .forEach((depto) => {
    db.usuarios.updateOne(
      { _id: depto.responsableId },
      { $set: { departamentoId: depto._id } },
    );
  });

