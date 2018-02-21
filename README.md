# 2dl
A definition language to represent type definitions.

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
  * Parameters: a list of strings or numbers (optional)

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
