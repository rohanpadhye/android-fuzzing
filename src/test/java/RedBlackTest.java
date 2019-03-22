import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import javafx.util.Pair;
import org.junit.runner.RunWith;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(JQF.class)
public class RedBlackTest {
    public boolean valid = true;

    public boolean isValidRedBlackTree(RedBlackTree tree) {
        valid = true;
        // Check if it is a valid binary search tree.
        if (!isValidBST(tree)) {
            return false;
        }

        // Check if all paths through the tree have the same number of black nodes.
        if (!pathLengthVerification(tree)) {
            return false;
        }

        // Check if all red nodes have a black parent.
        if (!redNodeBlackParent(tree)) {
            return false;
        }

        return true;
    }

    public boolean isValidBST(BinarySearchTree tree) {
        // Check if the BST's nodes are sorted
        BinaryTreeNode.Visitor v = new BinaryTreeNode.Visitor () {
            Object last = null;
            @Override
            public <E> void visit(BinaryTreeNode<E> node) {
                if (last == null) {
                    return;
                }
                if (tree.compare(last, node) != 1) {
                    valid = false;
                }
            }
        };
        tree.root.traverseInorder(v);
        return valid;
    }

    public boolean pathLengthVerification(RedBlackTree tree) {
        // Check that the number of black nodes along the path
        // from the root to any leaf is the same for all leaves.
        Stack<Pair<RedBlackTree.Node, Integer>> depths = new Stack<>();
        depths.push(new Pair<>((RedBlackTree.Node) tree.getRoot(), 0));
        int max_d = -1;
        while (depths.size() != 0) {
            Pair<RedBlackTree.Node, Integer> top = depths.peek();
            depths.pop();
            RedBlackTree.Node n = top.getKey();
            int d = top.getValue();
            if (n == null) {
                if (max_d == -1) {
                    max_d = d;
                } else {
                    if (d != max_d) {
                        return false;
                    }
                }
            } else {
                if (!n.isRed) {
                    d = d + 1;
                }
                depths.push(new Pair<>((RedBlackTree.Node) n.left, d));
                depths.push(new Pair<>((RedBlackTree.Node) n.right, d));
            }
        }
        return true;
    }

    public boolean redNodeBlackParent(RedBlackTree tree) {
        // Check that every red node has a black parent
        BinaryTreeNode.Visitor v = new BinaryTreeNode.Visitor() {
            @Override
            public <E> void visit(BinaryTreeNode<E> node) {
                if (node.getParent() != null) {
                    if (((RedBlackTree.Node) node).isRed && ((RedBlackTree.Node) node.getParent()).isRed) {
                        valid = false;
                    }
                } else if (((RedBlackTree.Node) node).isRed) {
                    valid = false;
                }
            }
        };
        tree.root.traversePreorder(v);
        return valid;
    }

    @Fuzz
    public void testAdd(@From(RedBlackGenerator.class) RedBlackTree tree, int d) {
        assumeTrue(isValidRedBlackTree(tree));
        tree.add(d);
        assertTrue(tree.contains(d));
        assertTrue(isValidRedBlackTree(tree));
    }

    @Fuzz
    public void testRemove(@From(RedBlackGenerator.class) RedBlackTree tree, int d) {
        assumeTrue(isValidRedBlackTree(tree));
        tree.remove(d);
        assertFalse(tree.contains(d));
        assertTrue(isValidRedBlackTree(tree));
    }

    @Fuzz
    public void testUnion(@Size(max=10) List<@From(RedBlackGenerator.class) RedBlackTree> trees) {
        RedBlackTree union = new RedBlackTree(Comparator.naturalOrder());
        for (RedBlackTree tree: trees) {
            assumeTrue(isValidRedBlackTree(tree));
            BinaryTreeNode.Visitor v = new BinaryTreeNode.Visitor() {
                @Override
                public <E> void visit(BinaryTreeNode<E> node) {
                    union.add(node.getData());
                }
            };
            tree.root.traversePreorder(v);
        }

        assertTrue(isValidRedBlackTree(union));
        for (RedBlackTree tree: trees) {
            BinaryTreeNode.Visitor v = new BinaryTreeNode.Visitor() {
                @Override
                public <E> void visit(BinaryTreeNode<E> node) {
                    assertTrue(union.contains(node.getData()));
                }
            };
            tree.root.traversePreorder(v);
        }
    }
}
