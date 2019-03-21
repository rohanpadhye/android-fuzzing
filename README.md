# Stateful Fuzzing Tutorial
In this tutorial, we will be going through a stateful data structure generator.

We will begin by using an implementation of a Red Black Tree (http://cs.lmu.edu/~ray/notes/redblacktrees/), and our goal is to use fuzzing to test the correctness of the implementation. Zest performs generator-based mutational fuzzing, with feedback from code coverage and input validity. For a specific application, this requires building a parametric generator of the object, as well as a test driver.

# Requirements
Java 8, Apache Maven, JQF Maven Plugin

# Test Driver
The tests that we will be using to verify correctness of the given Red Black Tree will test adding elements, removing elements, and taking the union of multiple trees. These tests are effectively testing the set-like properties of a Red Black Tree, and do not test the performance of the implementation, just correctness.

```
@Fuzz
public void testAdd(@From(RedBlackGeneratorDirect.class) RedBlackTree tree, int d) {
    assumeTrue(isValidRedBlackTree(tree));
    tree.add(d);
    assertTrue(tree.contains(d));
    assertTrue(isValidRedBlackTree(tree));
}
```

In the above test, the `@Fuzz` annotation tells JQF which tests can be used for fuzzing, that which require generating inputs. The `testAdd` method takes in two arguments here, where the first is annotated: `@From(RedBlackGeneratorDirect.class) RedBlackTree tree`. Since the inputs to this test are a parametrically generated, we need to specify from which generator should this object be generated from. In this case, we are generating a RedBlackTree from the RedBlackGeneratorDirect class, that contains an implementation of the `generate` method. The second argument, `int d`, does not require a special annotation because JQF already contains a generator for `int` type primitives, and this is the one we want to use.

Inside the method body, we start with our test's precondition, using JUnit's `assumeTrue`. Our precondition for this test is that the tree must be a valid Red Black Tree, which means that it satisfies the invariants that all valid Red Black Trees must satisfy. This is necessary because it may be possible that our generator does not generate semantically valid red black trees. If it is not valid, then JQF will throw this input away, and try to generate another one that is valid. Next, our test logic runs, in this case adding an element. Finally, our test's postconditions are asserted with `assertTrue`. This asserts that the tree does contain the element we just added, and that the tree is still a valid Red Black Tree.

Our test driver uses the method `isValidRedBlackTree`, which tests the tree invariants a valid RedBlackTree must satisfy: it is a binary search tree, each red node must have a black parent and the root is black, and the number of black nodes in the path from the root to a null child is the same for every null child.

# Generator

In the Test Driver section, we noted that the RedBlackTree was generated from JQF from the class `RedBlackGeneratorDirect`. This class contains relevant methods for stateful generation of RedBlackTrees, in that generation is not purely random: some of the generation is constrained to enforce RedBlackTree invariants.

In particular, the method `generate` is called by JQF for generation. One of the parameters, `SourceOfRandomness random`, is the parameterization of the tree, and it is this parameter that is mutated by JQF during fuzzing.

```
    @Override
    public RedBlackTree generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        int treeDepth = random.nextInt(0,100);
        RedBlackTree tree = new RedBlackTree(Comparator.naturalOrder());
        tree.setRoot(generateIntervalAux(random, tree, treeDepth, -K, K));
        return tree;
    }
``` 

Here, we construct a new `RedBlackTree`, and set its root node to be `generateIntervalAux(random, tree, treeDepth, -K, K)`.

```
    private RedBlackTree.Node generateIntervalAux(SourceOfRandomness random, RedBlackTree tree, int maxDepth, int min, int max) {
        int data = random.nextInt(min, max);
        RedBlackTree.Node node = tree.new Node(data);
        node.isRed = random.nextBoolean();

        if (random.nextBoolean() && maxDepth >= 0) {
            if (random.nextBoolean()) {
                node.setLeft(generateIntervalAux(random, tree, maxDepth - 1, min, data));
                node.setRight(generateIntervalAux(random, tree, maxDepth - 1, data, max));
            } else {
                if (random.nextBoolean()) {
                    node.setLeft(generateIntervalAux(random, tree, maxDepth - 1, min, data));
                } else {
                    node.setRight(generateIntervalAux(random, tree, maxDepth - 1, data, max));
                }
            }
        }
        return node;
    }
```

`generateIntervalAux` generates a `RedBlackTree` that satisfies the Binary Search Tree constraint. Note element generation `data = random.nextInt(min, max)` enforces the BST invariant. It assigns the node colors randomly, and does not guarantee satisfaction of the node color invariants that a RedBlackTree must satisfy.

This method also enforces a maxDepth on the tree. Each node is randomly chosen to be a leaf, and if it is not, then it is randomly chosen to have one or two children, who are recursively generated. 

# Results

The result of running blind fuzzing (without JQF's feedback) is the following:

Blind fuzzing
```
Zest: Validity Fuzzing with Parametric Generators
-------------------------------------------------
Test name:            RedBlackDirectTest#testAdd
Results directory:    /home/sirej/projects/redblacktrees/target/fuzz-results/RedBlackDirectTest/testAdd
Elapsed time:         1m 20s (no time limit)
Number of executions: 227,099
Valid inputs:         69,694 (30.69%)
Cycles completed:     0
Unique failures:      0
Queue size:           0 (0 favored last cycle)
Current parent input: <seed>
Execution speed:      3,130/sec now | 2,819/sec overall
Total coverage:       213 (0.33% of map)
Valid coverage:       208 (0.32% of map)
```

The result of running JQF fuzzing with feedback is the following:
```
Zest: Validity Fuzzing with Parametric Generators
-------------------------------------------------
Test name:            RedBlackDirectTest#testAdd
Results directory:    /home/sirej/projects/redblacktrees/target/fuzz-results/RedBlackDirectTest/testAdd
Elapsed time:         1m 20s (no time limit)
Number of executions: 193,203
Valid inputs:         87,569 (45.32%)
Cycles completed:     29
Unique failures:      0
Queue size:           35 (13 favored last cycle)
Current parent input: 15 (favored) {404/620 mutations}
Execution speed:      2,480/sec now | 2,406/sec overall
Total coverage:       227 (0.35% of map)
Valid coverage:       222 (0.34% of map)
```

There is a clear difference in code coverage. 


