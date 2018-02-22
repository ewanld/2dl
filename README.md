# 2dl
2dl (pronounced *toodle*) is a definition language to represent type definitions.

## Syntax
A ```.2dl``` file is a series of *definitions*.

* Each definition has:
  * A name
  * Modifiers (optional)
  * A type (optional)
  
  Example:
  ```
    public      a:  string
      ↑         ↑     ↑
   modifier   name   type
  ```

* Each type has:
  * A name
  * Type parameters (optional)
  * Type annotations (optional)
  * Sub-defintions (optional)
  
  Example, without sub-definitions:
  
  ```
      array<string>              minLength(2)           nullable
      ↑         ↑                     ↑                    ↑
   type name   type parameter   1st type annotation   2nd type annotation
  ```
  
  
  Example, with sub-definitions:
  ```
  type name
    ↓
  object {
    a: string  // 1st sub-definition
    b: int     // 2nd sub-definition
  }
  
  ```
  
* Each type annotation has:
  * A name
  * Parameters: a comma-separated list of strings, numbers or booleans, enclosed by parentheses. (optional; if missing, it is equivalent to the 'true' boolean value).

## Complete example
```
Individual: object {
	required firstName: string
	required lastName: string
	age: integer description("Age in years") min(0)
	accountNumber: accountNumber
}

Individuals: array<Individual>

accountNumber: string
```

In this example we define 3 types:
* ```Individual``` is a composite object
* ```Individuals``` represents a list of ```Individual```
* ```accountNumber``` is a type alias for a ```string```.

## 2dl schema
It is possible to validate a 2dl definitions against a schema. The schema itself is a 2dl file. While there are no semantics associated with 2dl definitions in general, a 2dl schema is defined by specific rules.
* The most basic definition has the form ```name: type```. It defines a new type with no type parameters, no children and no type annotations:

<table>
	<tr>
	<th>2dl schema</th>
	<th>2dl definition</th>
	<th>Valid?</th>
	</tr>
	<tr>
	<td rowspan="4"><pre>string: type</pre></td>
	<td><pre>a: string</pre></td>
	<td>✓</td>
	</tr>
	<tr>
	<td><pre>a: string&lt;int&gt;</pre></td>
	<td>Invalid: no type parameters allowed</td>
	</tr>
	<tr>
	<td><pre>
a: string {
	b: string
}</pre>
	</td>
	<td>Invalid: no sub-definitions allowed</td>
	</tr>
	<tr>
	<td><pre>a: string nullable</pre></td>
	<td>Invalid: no type annotations allowed</td>
	</tr>
</table>

## 2dl meta-schema
2dl schemas can be validated against the 2dl meta-schema:

```
// Allowed children types: annotation, modifier
type: type composite {
	typeParamCount: annotation<int>
	minTypeParamCount: annotation<int>
	maxTypeParamCount: annotation<int>
	composite: annotation<empty>
	extends: annotation<string>
}

annotation: type typeParamCount(1) {
	required: annotation<empty>
}

modifier: type

any: type
empty: type
number: type
int: type
string: type

variadic: type typeParamCount(1)
```
