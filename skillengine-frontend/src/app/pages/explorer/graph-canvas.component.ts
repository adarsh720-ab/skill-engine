import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import * as d3 from 'd3';
import { GraphData, GraphLink, GraphNode } from '../../core/models/skill.model';

type SimNode = GraphNode & d3.SimulationNodeDatum & { level?: number; targetY?: number };
type SimLink = d3.SimulationLinkDatum<SimNode> & { source: string | SimNode; target: string | SimNode };

const CATEGORY_COLORS: Record<string, string> = {
  Frontend: '#5eead4',
  Backend: '#f0b429',
  Language: '#7cc4ff',
  Framework: '#c792ea',
  Database: '#ff8a65',
  Security: '#f16565',
  'Cloud Platform': '#82e0aa',
  Tool: '#8892a6',
  Architecture: '#f9d976',
  Testing: '#66d9ef',
  Mobile: '#ff8fb1',
  'Data Science': '#b39ddb',
  'Data Engineering': '#80cbc4',
  Cybersecurity: '#ef9a9a',
  QA: '#90caf9',
  'Game Development': '#ffcc80',
  Web3: '#a5d6a7',
  Concept: '#ce93d8',
  Library: '#4dd0e1',
};

function colorFor(category: string): string {
  return CATEGORY_COLORS[category] ?? '#5eead4';
}

const COLUMN_WIDTH = 190;
const ROW_HEIGHT = 64;
const MARGIN_X = 70;
const MARGIN_Y = 60;

@Component({
  selector: 'app-graph-canvas',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="graph-host" #host>
      <svg #svg></svg>
      <div class="empty-hint" *ngIf="!data || data.nodes.length === 0">
        <span class="glyph">◈</span>
        <p>Search a skill to grow the roadmap.</p>
      </div>
    </div>
  `,
  styleUrl: './graph-canvas.component.scss',
})
export class GraphCanvasComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() data: GraphData | null = null;
  @Input() pathMode = false;
  @Output() nodeSelected = new EventEmitter<GraphNode>();

  @ViewChild('host', { static: true }) hostRef!: ElementRef<HTMLDivElement>;
  @ViewChild('svg', { static: true }) svgRef!: ElementRef<SVGSVGElement>;

  private simulation: d3.Simulation<SimNode, SimLink> | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private viewReady = false;

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.resizeObserver = new ResizeObserver(() => this.render());
    this.resizeObserver.observe(this.hostRef.nativeElement);
    this.render();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.viewReady && (changes['data'] || changes['pathMode'])) {
      this.render();
    }
  }

  ngOnDestroy(): void {
    this.simulation?.stop();
    this.resizeObserver?.disconnect();
  }

  private render(): void {
    const svgEl = this.svgRef.nativeElement;
    const host = this.hostRef.nativeElement;

    const svg = d3.select(svgEl);
    svg.selectAll('*').remove();
    this.simulation?.stop();

    if (!this.data || this.data.nodes.length === 0) {
      return;
    }

    const nodes: SimNode[] = this.data.nodes.map((n) => ({ ...n }));
    const links: SimLink[] = this.data.links.map((l: GraphLink) => ({ ...l }));

    this.assignLevels(nodes, links);

    const levelGroups = new Map<number, SimNode[]>();
    for (const n of nodes) {
      const lvl = n.level ?? 0;
      if (!levelGroups.has(lvl)) levelGroups.set(lvl, []);
      levelGroups.get(lvl)!.push(n);
    }

    const maxLevel = Math.max(...Array.from(levelGroups.keys()));
    const maxRows = Math.max(...Array.from(levelGroups.values()).map((g) => g.length));

    const width = Math.max(host.clientWidth || 600, MARGIN_X * 2 + (maxLevel + 1) * COLUMN_WIDTH);
    const height = Math.max(host.clientHeight || 480, MARGIN_Y * 2 + maxRows * ROW_HEIGHT);

    // Explicit pixel size (not width:100%/viewBox-scale-to-fit) so dense
    // roadmaps scroll inside the panel instead of shrinking to illegibility.
    svg.attr('width', width).attr('height', height).attr('viewBox', `0 0 ${width} ${height}`);

    // Fixed column x per level, evenly spaced target y within the level.
    // This is what makes the layout read like roadmap.sh instead of a
    // physics blob — x never moves, even while dragging.
    for (const [lvl, group] of levelGroups) {
      const x = MARGIN_X + lvl * COLUMN_WIDTH;
      const totalHeight = (group.length - 1) * ROW_HEIGHT;
      const startY = height / 2 - totalHeight / 2;
      group.forEach((n, i) => {
        n.fx = x;
        n.x = x;
        n.targetY = startY + i * ROW_HEIGHT;
        n.y = n.y ?? n.targetY;
      });
    }

    const container = svg.append('g').attr('class', 'zoom-layer');

    svg.call(
      d3
        .zoom<SVGSVGElement, unknown>()
        .scaleExtent([0.5, 2])
        .on('zoom', (event) => container.attr('transform', event.transform)),
    );

    if (this.pathMode) {
      svg
        .append('defs')
        .append('marker')
        .attr('id', 'arrow')
        .attr('viewBox', '0 -5 10 10')
        .attr('refX', 24)
        .attr('refY', 0)
        .attr('markerWidth', 6)
        .attr('markerHeight', 6)
        .attr('orient', 'auto')
        .append('path')
        .attr('d', 'M0,-5L10,0L0,5')
        .attr('fill', 'var(--color-highlight)');
    }

    const linkGenerator = d3
      .linkHorizontal<unknown, SimNode>()
      .x((d) => d.x ?? 0)
      .y((d) => d.y ?? 0);

    const linkSelection = container
      .append('g')
      .attr('class', 'links')
      .selectAll<SVGPathElement, SimLink>('path')
      .data(links)
      .join('path')
      .attr('class', this.pathMode ? 'link path-link' : 'link')
      .attr('fill', 'none')
      .attr('marker-end', this.pathMode ? 'url(#arrow)' : null);

    const nodeGroup = container
      .append('g')
      .attr('class', 'nodes')
      .selectAll<SVGGElement, SimNode>('g')
      .data(nodes)
      .join('g')
      .attr('class', 'node')
      .style('cursor', 'pointer')
      .on('click', (_event, d) => this.nodeSelected.emit(d))
      .call(this.dragBehavior());

    nodeGroup
      .append('circle')
      .attr('r', (d) => (d.isRoot || d.isEndpoint ? 14 : 9))
      .attr('fill', (d) => colorFor(d.category))
      .attr('class', (d) => (d.isRoot || d.isEndpoint ? 'node-circle emphasized' : 'node-circle'));

    nodeGroup
      .append('circle')
      .attr('r', (d) => (d.isRoot || d.isEndpoint ? 20 : 13))
      .attr('class', 'node-glow')
      .attr('fill', (d) => colorFor(d.category));

    nodeGroup
      .append('text')
      .attr('class', 'node-label')
      .attr('dy', (d) => (d.isRoot || d.isEndpoint ? 32 : 24))
      .attr('text-anchor', 'middle')
      .text((d) => d.id);

    this.simulation = d3
      .forceSimulation<SimNode>(nodes)
      .force('y', d3.forceY<SimNode>((d) => d.targetY ?? 0).strength(0.9))
      .force('collide', d3.forceCollide<SimNode>().radius(30))
      .alpha(1)
      .on('tick', () => {
        linkSelection.attr('d', (d) =>
          linkGenerator({ source: d.source as SimNode, target: d.target as SimNode }),
        );
        nodeGroup.attr('transform', (d) => `translate(${d.x ?? 0}, ${d.y ?? 0})`);
      });
  }

  /** BFS distance from the root determines each node's column, so the graph
   *  always reads left-to-right like a roadmap instead of everything
   *  radiating out of a single hub. */
  private assignLevels(nodes: SimNode[], links: SimLink[]): void {
    const byId = new Map(nodes.map((n) => [n.id, n]));
    const adjacency = new Map<string, string[]>();
    for (const l of links) {
      const s = typeof l.source === 'string' ? l.source : l.source.id;
      const t = typeof l.target === 'string' ? l.target : l.target.id;
      if (!adjacency.has(s)) adjacency.set(s, []);
      adjacency.get(s)!.push(t);
    }

    const rootNode = nodes.find((n) => n.isRoot) ?? nodes[0];
    const visited = new Set<string>([rootNode.id]);
    rootNode.level = 0;

    const queue: string[] = [rootNode.id];
    while (queue.length > 0) {
      const currentId = queue.shift()!;
      const currentLevel = byId.get(currentId)?.level ?? 0;
      for (const neighborId of adjacency.get(currentId) ?? []) {
        if (!visited.has(neighborId)) {
          visited.add(neighborId);
          const neighbor = byId.get(neighborId);
          if (neighbor) {
            neighbor.level = currentLevel + 1;
            queue.push(neighborId);
          }
        }
      }
    }

    // Anything BFS never reached (a disconnected fragment) still gets a
    // column so it renders instead of silently vanishing.
    const maxKnownLevel = Math.max(0, ...nodes.map((n) => n.level ?? 0));
    for (const n of nodes) {
      if (n.level === undefined) n.level = maxKnownLevel + 1;
    }
  }

  private dragBehavior() {
    return d3
      .drag<SVGGElement, SimNode>()
      .on('start', (event, d) => {
        if (!event.active) this.simulation?.alphaTarget(0.2).restart();
        d.fy = d.y;
      })
      .on('drag', (event, d) => {
        // x stays locked to the node's column — only vertical repositioning
        // is allowed, which is what keeps dragging from turning the roadmap
        // into a tangle.
        d.fy = event.y;
      })
      .on('end', (event, d) => {
        if (!event.active) this.simulation?.alphaTarget(0);
        d.fy = null;
      });
  }
}