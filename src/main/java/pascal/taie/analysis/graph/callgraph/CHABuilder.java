/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.World;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.MapUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds call graph via class hierarchy analysis.
 */
class CHABuilder implements CGBuilder<Invoke, JMethod> {

    private ClassHierarchy hierarchy;

    /**
     * Cache resolve results for interface/virtual invocations.
     */
    private Map<JClass, Map<MemberRef, Set<JMethod>>> resolveTable;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(World.getMainMethod());
        buildCallGraph(callGraph);
        return callGraph;
    }

    private void buildCallGraph(DefaultCallGraph callGraph) {
        hierarchy = World.getClassHierarchy();
        resolveTable = MapUtils.newMap();
        Queue<JMethod> queue = new LinkedList<>(callGraph.getEntryMethods());
        while (!queue.isEmpty()) {
            JMethod method = queue.remove();
            for (Invoke invoke : callGraph.getCallSitesIn(method)) {
                Set<JMethod> callees = resolveCalleesOf(invoke);
                callees.forEach(callee -> {
                    if (!callGraph.contains(callee)) {
                        queue.add(callee);
                    }
                    callGraph.addEdge(invoke, callee,
                            CGUtils.getCallKind(invoke));
                });
            }
        }
        hierarchy = null;
        resolveTable = null;
    }

    /**
     * Resolves callees of a call site via class hierarchy analysis.
     */
    private Set<JMethod> resolveCalleesOf(Invoke callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        CallKind kind = CGUtils.getCallKind(callSite);
        switch (kind) {
            case INTERFACE:
            case VIRTUAL: {
                JClass cls = methodRef.getDeclaringClass();
                Set<JMethod> callees = MapUtils.getMapMap(resolveTable, cls, methodRef);
                if (callees != null) {
                    return callees;
                }
                callees = hierarchy.getAllSubclassesOf(cls, true)
                        .stream()
                        .filter(Predicate.not(JClass::isAbstract))
                        .map(c -> hierarchy.dispatch(c, methodRef))
                        .collect(Collectors.toUnmodifiableSet());
                MapUtils.addToMapMap(resolveTable, cls, methodRef, callees);
                return callees;
            }
            case SPECIAL:
            case STATIC: {
                return Set.of(methodRef.resolve());
            }
            default:
                throw new AnalysisException("Failed to resolve call site: " + callSite);
        }
    }
}