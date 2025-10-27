import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'generatePages'
})
export class GeneratePagesPipe implements PipeTransform {

  transform(value: unknown, ...args: unknown[]): unknown {
    return null;
  }

}
