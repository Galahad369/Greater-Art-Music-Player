using System.Globalization;
using GreaterArt.Desktop;

int checks = 0;
void Check(bool condition, string message)
{
    checks++;
    if (!condition) throw new InvalidOperationException(message);
}
void Visible(GraphExplorationResult result, params int[] expected)
{
    Check(result.Visible.Select((visible, index) => (visible, index)).Where(item => item.visible)
        .Select(item => item.index).SequenceEqual(expected), $"Wrong visible nodes: expected {string.Join(",", expected)}.");
    Check(result.VisibleCount == expected.Length, "Visible count disagrees with mask.");
}
void Throws<T>(Action action) where T : Exception
{
    checks++;
    try { action(); }
    catch (T) { return; }
    throw new InvalidOperationException($"Expected {typeof(T).Name}.");
}

string[] titles = ["ÉTÉ 東京 Live.flac", "été 東京 studio.flac", "中文 夜曲.mp3", "ΑΘΉΝΑ Live.mp3", "Island.mp3", "Alone.mp3"];
ExplorationEdge[] edges = [new(0, 1, .9), new(1, 2, .7), new(2, 3, .5), new(3, 4, .3)];
var graph = new GraphExploration(titles, edges);
Visible(graph.Compute(), 0, 1, 2, 3, 4, 5);
Check(graph.Compute().EdgeIndices.SequenceEqual([0, 1, 2, 3]), "Edge order must follow input order.");
Visible(graph.Compute("  éTé\t東京\n"), 0, 1);
Visible(graph.Compute("e\u0301te\u0301"), 0, 1);
Visible(graph.Compute("中文 夜"), 2);
Visible(graph.Compute("αθήνα"), 3);
Visible(graph.Compute("東京 live"), 0);
Visible(graph.Compute("東京 absent"));
Visible(graph.Compute("\u3000\t\n"), 0, 1, 2, 3, 4, 5);
var previousCulture = CultureInfo.CurrentCulture;
try
{
    CultureInfo.CurrentCulture = new CultureInfo("tr-TR");
    Visible(graph.Compute("island"), 4);
}
finally { CultureInfo.CurrentCulture = previousCulture; }

Visible(graph.Compute(showOrphans: false), 0, 1, 2, 3, 4);
Visible(graph.Compute("Live", showOrphans: false));
Visible(graph.Compute(minimumStrength: .7, showOrphans: false), 0, 1, 2);
Check(graph.Compute(minimumStrength: .7).EdgeIndices.SequenceEqual([0, 1]), "Threshold must include equality.");
Check(graph.Compute(minimumStrength: 1).EdgeIndices.Length == 0, "Strong threshold retained a weak edge.");
Visible(graph.Compute(minimumStrength: 1, showOrphans: false));
Visible(graph.Compute(localCenter: 0, localDepth: 1), 0, 1);
Visible(graph.Compute(localCenter: 0, localDepth: 2), 0, 1, 2);
Visible(graph.Compute(localCenter: 0, localDepth: 3), 0, 1, 2, 3);
Visible(graph.Compute(localCenter: 3, localDepth: 2), 1, 2, 3, 4);
Visible(graph.Compute(localCenter: 5, localDepth: 3), 5);
Visible(graph.Compute(localCenter: 5, showOrphans: false));
Visible(graph.Compute(localCenter: 0, localDepth: 3, minimumStrength: .8), 0, 1);
Visible(graph.Compute("live", localCenter: 0, localDepth: 3), 0);
Visible(graph.Compute("studio", localCenter: 0));
Check(graph.Compute(localCenter: 0, localDepth: 2).EdgeIndices.SequenceEqual([0, 1]), "Local edges escaped neighborhood.");

GraphColorGroup[] groups = [new("", 0xff000000), new("東京", 0xff112233), new("live", 0xff445566), new("中文 夜", 0xff778899)];
var grouped = graph.Compute(groups: groups);
Check(grouped.GroupColors[0] == 0xff112233 && grouped.GroupColors[1] == 0xff112233, "First matching group must win.");
Check(grouped.GroupColors[2] == 0xff778899, "Group query must use AND tokens.");
Check(grouped.GroupColors[3] == 0xff445566 && grouped.GroupColors[4] is null, "Unmatched groups should preserve default color.");
Check(graph.Compute("中文", groups: groups).GroupColors[0] is null, "Hidden nodes should not need a group color.");
Check(graph.Compute(groups: groups.Reverse().ToArray()).GroupColors[0] == 0xff445566, "Changing group priority should change first match.");

var full = graph.Compute();
Check(full.Degrees.SequenceEqual([1, 2, 2, 2, 1, 0]), "Visible degrees are incorrect.");
Check(Math.Abs(full.Weights[1] - 1) < 1e-12 && full.Weights[5] == 0, "Weights must be normalized visible connection strength.");
var filtered = graph.Compute(minimumStrength: .8);
Check(filtered.Degrees.SequenceEqual([1, 1, 0, 0, 0, 0]), "Hidden edges must not contribute to degrees.");
Check(filtered.Weights.SequenceEqual([1d, 1d, 0d, 0d, 0d, 0d]), "Weights must reflect remaining links.");
var repeat = graph.Compute(groups: groups);
Check(grouped.Visible.SequenceEqual(repeat.Visible) && grouped.EdgeIndices.SequenceEqual(repeat.EdgeIndices) &&
    grouped.Degrees.SequenceEqual(repeat.Degrees) && grouped.Weights.SequenceEqual(repeat.Weights) &&
    grouped.GroupColors.SequenceEqual(repeat.GroupColors), "Identical settings must give deterministic results.");
titles[0] = "changed";
edges[0] = new(4, 5, 1);
Visible(graph.Compute("東京"), 0, 1);
Check(graph.Compute().EdgeIndices.Length == 4, "Caller mutation must not change the graph snapshot.");

var empty = new GraphExploration([], []);
Visible(empty.Compute());
var zeroStrength = new GraphExploration(["a", "b"], [new(0, 1, 0)]).Compute(showOrphans: false);
Visible(zeroStrength, 0, 1);
Check(zeroStrength.Weights.All(double.IsFinite), "Zero strengths must not produce NaN weights.");
var selfEdge = new GraphExploration(["a"], [new(0, 0, 1)]).Compute(localCenter: 0, localDepth: 3);
Visible(selfEdge, 0);
Check(selfEdge.Degrees[0] == 2, "An undirected self-edge contributes twice to degree.");
Throws<ArgumentException>(() => new GraphExploration(["a"], [new(-1, 0, 1)]));
Throws<ArgumentException>(() => new GraphExploration(["a"], [new(0, 1, 1)]));
Throws<ArgumentException>(() => new GraphExploration(["a"], [new(0, 0, double.NaN)]));
Throws<ArgumentException>(() => new GraphExploration(["a"], [new(0, 0, double.PositiveInfinity)]));
Throws<ArgumentException>(() => new GraphExploration(["a"], [new(0, 0, -.1)]));
Throws<ArgumentException>(() => new GraphExploration(["a"], [new(0, 0, 1.1)]));
Throws<ArgumentException>(() => new GraphExploration([null!], []));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(localCenter: -1));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(localCenter: 6));
Throws<ArgumentOutOfRangeException>(() => empty.Compute(localCenter: 0));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(localDepth: 0));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(localDepth: 4));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(minimumStrength: double.NaN));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(minimumStrength: double.PositiveInfinity));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(minimumStrength: -.1));
Throws<ArgumentOutOfRangeException>(() => graph.Compute(minimumStrength: 1.1));

Console.WriteLine($"Graph exploration: {checks}/{checks} checks passed.");
