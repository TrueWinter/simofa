export function toCamelCase(string: string): string {
	let stringParts = string.split('_');
	let output = [stringParts.splice(0, 1)[0]];

	// A for of loop would be better here but then I get
	// an error: Expected 2 arguments, but got 1.
	// Apparently I'm the only one in the history of TypeScript
	// to ever get this error with a for of loop, as there's 
	// no information about this specific issue online.
	for (let i = 0; i < stringParts.length; i++) {
		let firstChar = stringParts[i].charAt(0);
		let restOfPart = stringParts[i].substring(1);

		output.push(`${firstChar.toUpperCase()}${restOfPart}`);
	}

	return output.join('');
}