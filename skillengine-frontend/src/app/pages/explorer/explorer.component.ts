import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GraphData, GraphNode, SkillStackResponse } from '../../core/models/skill.model';
import { SkillService } from '../../core/services/skill.service';
import { GraphCanvasComponent } from './graph-canvas.component';

@Component({
  selector: 'app-explorer',
  standalone: true,
  imports: [CommonModule, FormsModule, GraphCanvasComponent],
  templateUrl: './explorer.component.html',
  styleUrl: './explorer.component.scss',
})
export class ExplorerComponent {
  private readonly skillService = inject(SkillService);

  readonly query = signal('');
  readonly maxHops = signal(3);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly stacks = signal<SkillStackResponse[]>([]);
  readonly selectedNode = signal<GraphNode | null>(null);

  // Whether a search has ever been run — controls the search-first -> explorer collapse.
  readonly hasSearched = computed(() => this.stacks().length > 0 || this.errorMessage() !== null);

  readonly graphData = computed<GraphData>(() => this.buildGraph(this.stacks()));

  search(): void {
    const q = this.query().trim();
    if (!q) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.selectedNode.set(null);

    this.skillService.findRelated(q, this.maxHops()).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.stacks.set(res);
        if (res.length === 0) {
          this.errorMessage.set(`No skills found matching "${q}".`);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.stacks.set([]);
        this.errorMessage.set(
          err.status === 0
            ? 'Cannot reach the SkillEngine API. Is the backend running?'
            : 'Something went wrong while searching.',
        );
      },
    });
  }

  selectNode(node: GraphNode): void {
    this.selectedNode.set(node);
  }

  private buildGraph(stacks: SkillStackResponse[]): GraphData {
    const nodesById = new Map<string, GraphNode>();
    const linkKeys = new Set<string>();
    const links: GraphData['links'] = [];

    for (const stack of stacks) {
      if (!nodesById.has(stack.rootSkill)) {
        nodesById.set(stack.rootSkill, {
          id: stack.rootSkill,
          category: stack.rootCategory,
          isRoot: true,
        });
      }

      for (const related of stack.relatedSkills) {
        if (!nodesById.has(related.name)) {
          nodesById.set(related.name, { id: related.name, category: related.category });
        }
      }

      // Use the real PREREQUISITE_FOR edges within this subgraph — not a
      // root-to-every-related-skill fan-out — so multi-hop chains render as
      // actual chains instead of everything radiating from the root.
      for (const edge of stack.edges) {
        const key = `${edge.source}->${edge.target}`;
        if (!linkKeys.has(key)) {
          linkKeys.add(key);
          links.push({ source: edge.source, target: edge.target });
        }
      }
    }

    return { nodes: Array.from(nodesById.values()), links };
  }
}
