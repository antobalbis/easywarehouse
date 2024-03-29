package eWarehouse

import io.undertow.Undertow
import org.scalatest.funsuite.AnyFunSuite

class CMtests extends AnyFunSuite{

  var cm : ControladorMaquinas = new ControladorMaquinas()
  var lt : List[Trabajador] = List()
  lt = new Trabajador(0, "aa", Departamento.DIRECCION) :: lt
  lt = new Trabajador(1, "ab", Departamento.LOGISTICA) :: lt
  lt = new Trabajador(2, "ac", Departamento.MANTENIMIENTO) :: lt
  lt = new Trabajador(3, "ax", Departamento.LOGISTICA) :: lt

  cm.listaTrabajadores = lt

  val server = Undertow.builder
    .addHttpListener(8081, "localhost")
    .setHandler(cm.defaultHandler)
    .build
  server.start()

  val host = "http://localhost:8081"

  for(i <- 0 to 6){
    requests.post(s"$host/addMaquina",
      data = """{"userID_": [1], "id_":[""" + i + """], "nombre": "maquina"}""")
  }

  //TEST ADD MAQUINAS
  test("Lista de máquinas debe tener 5 elementos") {
    val n = requests.get(s"$host/getNMaquinas").text()
    assert(n == "7")
  }

  test("No se añade una máquina con id repetido"){
    val result = requests.post(s"$host/addMaquina",
      data = """{"userID_": [1], "id_":[2], "nombre": "maquina"}""")
    val n = requests.get(s"$host/getNMaquinas").text()
    assert(n == "7")
  }

  test("Comprobar que no se añade una máquina si el usuario no existe o no es del departamento de logística."){
    val result = requests.post(s"$host/addMaquina",
      data = """{"userID_": [7], "id_":[8], "nombre": "maquina"}""")

    val n = requests.get(s"$host/existMaquina/8").text().toBoolean
    assert(!n)
  }

  //TEST LISTA MAQUINAS
  test("No se muestra ninguna máquina con estado distinto a FUNCIONANDO"){
    val cambio = requests.put(s"$host/cambiarEstado",
      data = """{"userID" : 2, "id" : 3, "estadoMaquina_" : "REPARACION"}""")

    val result = ujson.read(requests.get(s"$host/getListaMaquinas/1"))
    var contiene : Boolean = true
    for(i <- 0 to result.arr.size - 1){
      if(contiene) contiene = result(i)("estado").str.equals("FUNCIONANDO")
    }
    assert(contiene)
  }

  test("Comprobar que se muestran todas las máquinas con estado FUNCIONANDO"){
    val result = ujson.read(requests.get(s"$host/getListaMaquinas/1"))
    assert(result.arr.size == 6)
  }

  test("Comprobar que si el usuario no existe el resultado es una lista vacía"){
    val result = ujson.read(requests.get(s"$host/getListaMaquinas/12"))
    assert(result.arr.size == 0)
  }

  //TESTS ELIMINA MAQUINA
  test("EL NUMERO DE MAQUINAS SE REDUCE EN UNO"){
    val n = requests.get(s"$host/getNMaquinas").text().toInt
    val result = requests.post(s"$host/rmMaquina", data = """{"userID" : [1], "id" : [4]}""")
    val n2 = requests.get(s"$host/getNMaquinas").text().toInt
    assert(n2 + 1 == n)
  }

  test("La máquina que desaparece es la máquina con el ID correspondiente"){
    val result = requests.get(s"$host/existMaquina/4").text().toBoolean
    assert(!result)
  }

  test("El número de máquinas no decrece si el ID no existe"){
    val n = requests.get(s"$host/getNMaquinas/").text().toInt
    val result = requests.post(s"$host/rmMaquina", data = """{"userID" : [1], "id" : [4]}""")
    val n2 = requests.get(s"$host/getNMaquinas").text().toInt
    assert(n == n2)
  }

  test("Comprobar que la máquina no se elimina si el usuario no existe"){
    val result = requests.post(s"$host/rmMaquina", data = """{"userID" : [12], "id" : [2]}""")
    val existe = requests.get(s"$host/existMaquina/2").text().toBoolean
    assert(existe)
  }

  test("Comprobar que la máquina no se elimina si el usuario no pertenece a logística"){
    val result = requests.post(s"$host/rmMaquina", data = """{"userID" : [2], "id" : [2]}""")
    val existe = requests.get(s"$host/existMaquina/2").text().toBoolean
    assert(existe)
  }

  //TESTS USO MAQUINA
  test("El estado de la máquina con el id especificado cambia a true"){
    requests.put(s"$host/usarMaquina", data = """{"id" : 0, "userId": 1}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/0"))
    assert(result.apply("uso").toString().toBoolean)
  }

  test("Comprobar que si el valor de isBeingUsed es true el resultado es false"){
    requests.put(s"$host/usarMaquina", data = """{"id" : 0, "userId": 3}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/0"))
    assert(!result.apply("userID").str.equals("3"))
  }

  test("Comprobar que si el estado de la máquina es distinto a FUNCIONANDO el resultado es false"){
    requests.put(s"$host/usarMaquina", data = """{"id" : 3, "userId": 3}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/3"))
    assert(!result.apply("uso").toString().toBoolean)
  }

  test("Comprobar si el resultado es true que el id del usuario es igual al id del usuario de la máquina."){
    val result = ujson.read(requests.get(s"$host/getMaquina/0"))
    assert(result.apply("userID").str.toInt == 1)
  }

  test("Comprobar que si el usuario no existe o no es del departamento de logística el resultado es false"){
    requests.put(s"$host/usarMaquina", data = """{"id" : 1, "userId": 12}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/1"))
    assert(!result.apply("uso").toString().toBoolean)

    requests.put(s"$host/usarMaquina", data = """{"id" : 1, "userId": 2}""")
    val result2 = ujson.read(requests.get(s"$host/getMaquina/1"))
    assert(!result.apply("uso").toString().toBoolean)
  }

  //TESTS DEJAR MÁQUINA
  test("Comprobar que si el resultado es true el valor de isBeingUsed es false y userID a -1"){
    requests.put(s"$host/dejarMaquina", data = """{"id" : 0, "userID" : 1}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/0"))

    assert(!result.apply("uso").toString().toBoolean)
    assert(result.apply("userID").str.equals("-1"))
  }

  test("Comprobar que si no coincide el userID con el userID de la máquina sigue en uso"){
    requests.put(s"$host/usarMaquina", data = """{"id" : 0, "userId": 1}""")
    requests.put(s"$host/dejarMaquina", data = """{"id" : 0, "userID" : 3}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/0"))
    assert(result.apply("uso").toString().toBoolean)
  }

  //Cambia estado máquina
  test("Comprobar que el estado de la máquina con el id indicado cambia al seleccionado"){
    requests.put(s"$host/cambiarEstado", data = """{"userID" : 2, "id" : 6, "estadoMaquina_" : "REPARACION"}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/6"))

    assert(result.apply("estado").str.equals(EstadoMaquina.REPARACION.toString))
    requests.put(s"$host/cambiarEstado", data = """{"userID" : 2, "id" : 6, "estadoMaquina_" : "FUNCIONANDO"}""")
  }

  test("Comprobar que si el trabajador no es de mantenimiento el estado no cambia."){
    requests.put(s"$host/cambiarEstado",
      data = """{"userID" : 1, "id" : 6, "estadoMaquina_" : "REPARACION"}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/6"))

    assert(!result.apply("estado").str.equals(EstadoMaquina.REPARACION.toString))
  }

  //Máquina deja de estar en funcionamiento
  test("Comprobar que el estado de la máquina con el id indicado cambia a pendiente."){
    requests.put(s"$host/averiar", data = """{"userID" : 1, "id" : 6}""")
    val result = ujson.read(requests.get(s"$host/getMaquina/6"))

    assert(result.apply("estado").str.equals(EstadoMaquina.PENDIENTE.toString))
  }

  test("Comprobar que si el estado de la máquina no es en funcionamiento no cambia."){
    requests.put(s"$host/cambiarEstado", data = """{"userID" : 2, "id" : 6, "estadoMaquina_" : "REPARACION"}""")
    requests.put(s"$host/averiar", data = """{"userID" : 1, "id" : 6}""")

    val result = ujson.read(requests.get(s"$host/getMaquina/6"))
    assert(result.apply("estado").str.equals(EstadoMaquina.REPARACION.toString))
  }

  test("Comprobar que no cambia el estado si el id del trabajador no existe."){
    requests.put(s"$host/averiar", data = """{"userID" : -11, "id" : 5}""")

    val result = ujson.read(requests.get(s"$host/getMaquina/5"))
    assert(result.apply("estado").str.equals(EstadoMaquina.FUNCIONANDO.toString))
  }
}
