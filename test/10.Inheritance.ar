import stdlib

class A {

	func a() {
		print("A::a()")
	}

	func foo() {
		print("A::foo()")	
	}
}

class B: A {

	override func a() {
		print("B::a()")
	}

	func b() {
		print("B::b()")
	}
}

class C: B {

	override func a() {
		print("C::a()")
	}
}

########################
#   Dynamic dispatch   #
########################

var a: A

let x = 30
if x == 10 {
	a = A()
}
else if x == 20 {
	a = B()
}
else {
	a = C()
}

a.a()

for i in [B(), A(), C(), C(), B()] {
	i.a()
	i.foo()
}

########################
#     Type casting     #
########################

var object: A
object = B()

print(object is B)

let casted = object as B
if casted != null {
	casted.b()
}
else {
	print("cast failed")
}

########################
#  Static definitions  #
########################

class Static {
	
	class A {
		let x = 10
		var str = "Danes je lep dan"

		func foo() {
		}

		init(str: String, x: Int) {
			self.str = str
			self.x = x
		}
	}

	enum Test: String {
		case a, b, c
	}

	static var x: Int = 20

	static func static_foo() {
		print("This is a static function")
		
		let obj = Static()
		Static.x = -100
		print(Static.x)
	}
}

class Tmp {
	func a() {
		print("a")
	}
	func b() {
		print("b")
	}
	func c() {
		print("c")
	}
}

Tmp().c()
Tmp().b()
Tmp().a()

Static.x = 10
print(Static.x)

print(Static.Test.a.rawValue)

Static.static_foo()

var dela = Static.A()
print(dela.str)
print(dela.x)

dela = Static.A(str: "Hello world", x: -300)
print(dela.str)
print(dela.x)


########################
#      Extensions      #
########################

extension A {

	func toString() {
		print("ext::toString()")
	}

	func toString(_ par: Int) {
		print(par)
		print("ext::toString(_:)")
	}
}

A().a()
B().toString()
C().toString(10)

extension Int {
	
	static func max() Int {
		return 2000000
	}

	func add(incr: Int) Int {
		return self + incr
	}
}

var int = 10
print(Int.max())
print(int.add(incr: 20))


########################
#      Interfaces      #
########################

interface P1 {
    func p()
}
interface P2 {
    func q()
}
interface P3 {
	func foo(x: Int)
}

class A2: P1, P2 {
    func p() {
    	print("This is A2's implementation of P1::p")
    }

    func q() {
    	print("This is A2's implementation of P2::q")
    }
}

class B2 {
	func p() {
    	print("This is B2's implementation of P1")
    }
}

let a2 = A2()
a2.p()
a2.q()
let b2 = B2()
b2.p()

/*if a2 is P1 {
	print("a2 is P1")
}
else {
	print("a2 is not P1")
}

if a2 is P1 && a2 is P2 {
	print("a2 is P1 and P2")
}
else {
	print("a2 is not P1 and P2")
}*/

########################
# Interface extensions #
########################

let h = 4232
print(h.hash())

extension A: Hashable {
	func hash() Int {
		return 10
	}
}

print(A().hash())

var hashable: Hashable
hashable = 10

let hashed: [Hashable] = [C(), B(), 10, 20, "12312"]

#hashed[0].hash() TODO


#hashable.hash() TODO
#print(bool: 10 is Int) TODO

# TODO
#let p1: P1
#p1 = A2()
#p1.p()
