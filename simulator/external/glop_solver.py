import ortoolslpparser
import sys


def solve(filename):

    parse_result = ortoolslpparser.parse_lp_file(filename)
    solver = parse_result["solver"]
    result = solver.Solve()

    if result == solver.OPTIMAL:
        print("Value of objective function: %f" % solver.Objective().Value())
        print("Actual values of the variables:")
        for var_name in parse_result["var_names"]:
            print("%s %.10f" % (var_name, solver.LookupVariable(var_name).solution_value()))
    else:

        print("Linear program was not solved.")
        error_msg = "UNKNOWN"
        if result == solver.OPTIMAL:
            error_msg = "OPTIMAL"
        elif result == solver.FEASIBLE:
            error_msg = "FEASIBLE"
        elif result == solver.INFEASIBLE:
            error_msg = "INFEASIBLE"
        elif result == solver.UNBOUNDED:
            error_msg = "UNBOUNDED"
        elif result == solver.ABNORMAL:
            error_msg = "ABNORMAL"
        elif result == solver.NOT_SOLVED:
            error_msg = "NOT SOLVED"
        print("Error result provided by OR-tools: %s (%d)" % (error_msg, result))
        exit(1)


def main():
    args = sys.argv[1:]
    if len(args) != 1:
        print("Usage: python3 glop_solver.py </path/to/program.lp>")
    else:
        solve(args[0])


if __name__ == "__main__":
    main()
